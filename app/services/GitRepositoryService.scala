package services

import java.io.File

import javax.inject.{Inject, Singleton}
import model.CustomExecutionContext.executionContextExecutor
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import play.api.{ConfigLoader, Configuration, Logger}

import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Try

@Singleton
class GitRepositoryService @Inject()(configuration: Configuration,
                                     gitHosts: Set[GitHost]) {

  private val projectUrl = "https?://[^ /]+/(.*)".r

  /**
    * Update all the repositories.
    */
  def updateGitRepositories(): Unit = {
    val path = new File(getProperty("project.repositories.path"))
    if (!path.exists()) {
      // creating workspace directory
      path.mkdirs()
    }

    val updatedRepositories: Seq[Future[Unit]] = fetchRepositories().map(updateGitRepository)
    val sequence = Future.sequence(updatedRepositories)

    // waiting for all the futures
    val timeout = configuration.get("timeout.git-update")(ConfigLoader.finiteDurationLoader)
    Await.result(sequence, timeout)
  }

  /**
    * Update a git repository.
    *
    * @param repository the repository
    */
  private def updateGitRepository(repository: GitRepository): Future[Unit] = Future {
    val repositoryUrl = repository.url
    projectUrl.findFirstMatchIn(repositoryUrl) match {
      case Some(value) => updateGitRepository(repository, value.group(1))
      case _           => Logger.error(s"error with $repositoryUrl")
    }
  }

  private def updateGitRepository(repository: GitRepository, repositoryName: String): Unit = Try {

    val repositoryDirectory = new File(getProperty("project.repositories.path"), repositoryName)

    if (repositoryDirectory.exists()) {
      pullRepository(repository, repositoryDirectory)
    } else {
      cloneRepository(repository, repositoryDirectory)
    }
  }

  /**
    * Update a git repository using git pull.
    *
    * @param repository          the repository
    * @param repositoryDirectory the repository directory
    */
  private def pullRepository(repository: GitRepository, repositoryDirectory: File): Unit = {
    val repositoryUrl = repository.url
    val git = new Git(new FileRepository(repositoryDirectory.getAbsolutePath + "/.git"))

    val command = git
      .pull()
      .setRebase(true)
      .setCredentialsProvider(getUserCredentials(repository))

    try {
      command.call()
      Logger.info(s"$repositoryUrl repository pulled")
    } catch {
      // empty repository
      case _: RefNotAdvertisedException => Logger.warn(s"Skipping repository $repositoryUrl (empty repository)")
      case e: Exception => Logger.error(s"Skipping repository $repositoryUrl", e)
    }
  }

  /**
    * Clone a git repository into workspace.
    *
    * @param repository          the repository
    * @param repositoryDirectory the repository directory
    */
  private def cloneRepository(repository: GitRepository, repositoryDirectory: File): Unit = {
    val repositoryUrl = repository.url
    Logger.info(s"Cloning repository $repositoryUrl")

    val cloneCommand = Git.cloneRepository

    cloneCommand.setURI(repositoryUrl + ".git")

    cloneCommand.setCredentialsProvider(getUserCredentials(repository))
    cloneCommand.setDirectory(repositoryDirectory)

    try {
      cloneCommand.call()
      Logger.info(s"$repositoryUrl repository cloned")
    } catch {
      case e: Exception => Logger.error(s"Skipping repository $repositoryUrl", e)
    }
  }

  /**
    * Fetch every repository information.
    *
    * @return a sequence of all the repositories
    */
  private def fetchRepositories(): Seq[GitRepository] = {
    // todo(all): retrieve the urls in parallel
    gitHosts
      .flatMap(_.getRepositories)
      .toSeq
  }

  private def getProperty(property: String): String = configuration.getOptional[String](property).getOrElse("")

  private def getUserCredentials(repository: GitRepository) = {
    new UsernamePasswordCredentialsProvider(repository.user, repository.token)
  }
}
