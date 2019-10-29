package services

import java.io.File
import java.nio.file.Files
import java.util.Comparator

import executors.pool
import javax.inject.{Inject, Singleton}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import play.api.{Configuration, Logger}
import scalaz.concurrent.Task
import utils.TaskHelper.gather

import scala.language.postfixOps

@Singleton
class GitRepositoryService @Inject()(configuration: Configuration,
                                     gitHosts: Set[GitHost]) {

  private val projectUrl = "https?://[^ /]+/(.*)".r
  private val logger = Logger(classOf[GitRepositoryService])

  /**
    * Update all the repositories.
    */
  def updateGitRepositories(): Task[_] = {
    val path = new File(getProperty("project.repositories.path"))
    if (!path.exists()) {
      // creating workspace directory
      path.mkdirs()
    }

    for {
      repos <- fetchRepositories()
      updatedRepositories <- gather(repos.flatten.map(updateGitRepository))
    } yield updatedRepositories
  }

  /**
    * Update a git repository.
    *
    * @param repository the repository
    */
  private def updateGitRepository(repository: GitRepository): Task[_] = {
    val repositoryUrl = repository.url
    projectUrl.findFirstMatchIn(repositoryUrl) match {
      case Some(value) => updateGitRepository(repository, value.group(1))
      case _           => Task.now(logger.error(s"error with $repositoryUrl"))
    }
  }

  private def updateGitRepository(repository: GitRepository, repositoryName: String): Task[_] = {

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
  private def pullRepository(repository: GitRepository, repositoryDirectory: File): Task[_] = Task {
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
      case e: Exception =>
        logger.error(s"Skipping repository $repositoryUrl", e)
        repositoryDirectory.deleteRecursively()
    }
  }

  /**
    * Clone a git repository into workspace.
    *
    * @param repository          the repository
    * @param repositoryDirectory the repository directory
    */
  private def cloneRepository(repository: GitRepository, repositoryDirectory: File): Task[_] = Task {
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
  private def fetchRepositories(): Task[List[Seq[GitRepository]]] = gather(gitHosts.map(host => Task { host.getRepositories }))

  private def getProperty(property: String): String = configuration.getOptional[String](property).getOrElse("")

  private def getUserCredentials(repository: GitRepository) = {
    new UsernamePasswordCredentialsProvider(repository.user, repository.token)
  }

  private implicit class FileExtension(file: File) {
    def deleteRecursively(): Unit = Files.walk(file.toPath)
      .sorted(Comparator.reverseOrder())
      .forEach(path => path.toFile.delete())
  }
}
