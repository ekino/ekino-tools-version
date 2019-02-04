package services

import java.io.File

import javax.inject.{Inject, Singleton}
import model.CustomExecutionContext.executionContextExecutor
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import play.api.{Configuration, Logger}
import utils.FutureHelper

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try

@Singleton
class GitRepositoryService @Inject()(configuration: Configuration,
                                     gitHosts: Set[GitHost]) {

  private val projectUrl = "https?://[^ /]+/(.*)".r
  private val logger = Logger(classOf[GitRepositoryService])

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

    FutureHelper.await[Unit](updatedRepositories, configuration, "timeout.git-update")
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
      case _           => logger.error(s"error with $repositoryUrl")
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
    logger.info(s"Pulling repository $repositoryUrl")

    val git = new Git(new FileRepository(repositoryDirectory.getAbsolutePath + "/.git"))

    val command = git
      .pull()
      .setRebase(true)
      .setCredentialsProvider(getUserCredentials(repository))

    try {
      command.call()
      logger.info(s"$repositoryUrl repository pulled")
    } catch {
      // empty repository
      case _: RefNotAdvertisedException => logger.warn(s"Skipping repository $repositoryUrl (empty repository)")
      case e: Exception => logger.error(s"Skipping repository $repositoryUrl", e)
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
    logger.info(s"Cloning repository $repositoryUrl")

    val cloneCommand = Git.cloneRepository

    cloneCommand.setURI(repositoryUrl + ".git")

    cloneCommand.setCredentialsProvider(getUserCredentials(repository))
    cloneCommand.setDirectory(repositoryDirectory)

    try {
      cloneCommand.call()
      logger.info(s"$repositoryUrl repository cloned")
    } catch {
      case e: Exception => logger.error(s"Skipping repository $repositoryUrl", e)
    }
  }

  /**
    * Fetch every repository information.
    *
    * @return a sequence of all the repositories
    */
  private def fetchRepositories(): Seq[GitRepository] = {
    val repositories = gitHosts
      .map(host => Future { host.getRepositories })
      .toSeq

    FutureHelper.await[Seq[GitRepository]](repositories, configuration, "timeout.fetch-repositories").flatten
  }

  private def getProperty(property: String): String = configuration.getOptional[String](property).getOrElse("")

  private def getUserCredentials(repository: GitRepository) = {
    new UsernamePasswordCredentialsProvider(repository.user, repository.token)
  }
}
