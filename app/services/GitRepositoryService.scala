package services

import java.io.File
import java.net.URL

import javax.inject.{Inject, Singleton}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import play.api.ConfigLoader.stringLoader
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source

@Singleton
class GitRepositoryService @Inject()(configuration: Configuration) {

  /**
    * Update all the repositories.
    */
  def updateGitRepositories(): Unit = {
    val path = new File(getProperty("project.repositories.path"))
    if (!path.exists()) {
      // creating workspace directory
      path.mkdirs()
    }

    val sequence = Future.sequence(fetchRepositoryNames().map(updateGitRepository))

    // waiting for all the futures
    Await.result(sequence, Duration.Inf)
  }

  /**
    * Update a git repository.
    *
    * @param repositoryName the repository name
    */
  private def updateGitRepository(repositoryName: String): Future[Unit] = Future {
    val repositoryDirectory = new File(getProperty("project.repositories.path"), repositoryName)

    if (repositoryDirectory.exists()) {
      pullRepository(repositoryName, repositoryDirectory)
    } else {
      cloneRepository(repositoryName, repositoryDirectory)
    }
  }

  /**
    * Update a git repository using git pull.
    *
    * @param repositoryName      the repository name
    * @param repositoryDirectory the repository directory
    */
  private def pullRepository(repositoryName: String, repositoryDirectory: File): Unit = {
    val git = new Git(new FileRepository(repositoryDirectory.getAbsolutePath + "/.git"))

    val command = git
      .pull()
      .setRebase(true)
      .setCredentialsProvider(getUserCredentials)

    try {
      command.call()
      Logger.info(s"$repositoryName repository pulled")
    } catch {
      // empty repository
      case _: RefNotAdvertisedException => Logger.warn(s"Skipping repository $repositoryName (empty repository)")
      case e: Exception => Logger.error(s"Skipping repository $repositoryName", e)
    }
  }

  /**
    * Clone a git repository into workspace.
    *
    * @param repositoryName      the repository name
    * @param repositoryDirectory the repository directory
    */
  private def cloneRepository(repositoryName: String, repositoryDirectory: File): Unit = {
    Logger.info(s"Cloning repository $repositoryName")

    val cloneCommand = Git.cloneRepository

    cloneCommand.setURI(getProperty("gitlab.url") + "/" + repositoryName + ".git")

    cloneCommand.setCredentialsProvider(getUserCredentials)
    cloneCommand.setDirectory(repositoryDirectory)

    try {
      cloneCommand.call()
      Logger.info(s"$repositoryName repository cloned")
    } catch {
      case e: Exception => Logger.error(s"Skipping repository $repositoryName", e)
    }
  }

  /**
    * Fetch the repository names using gitlab api.
    *
    * @return a sequence of all the repository names
    */
  private def fetchRepositoryNames(): Seq[String] = {
    Logger.info("Fetching repositories from gitlab")

    val groupIds = getProperty("gitlab.group.ids")

    groupIds
      .split(',')
      .filter(_.nonEmpty)
      .flatMap(groupId => fetchRepositoryNames(groupId))
  }

  /**
    * Fetch the repository names for the given groupId using gitlab api.
    *
    * @param groupId The gitlab group id
    * @return a sequence of all the repository names
    */
  private def fetchRepositoryNames(groupId: String): Seq[String] = {

    // gitlab api url
    val url: String = getProperty("gitlab.url") + "/api/v4/groups/" + groupId + "/projects?per_page=1000"

    val connection = new URL(url).openConnection
    connection.setRequestProperty("PRIVATE-TOKEN", getProperty("gitlab.token"))

    val result = Source.fromInputStream(connection.getInputStream).mkString

    // getting the project list of gitlab group
    val repositories = Json.parse(result)
    val ignored = getPropertyList("gitlab.ignored.repository")
    val additionalRepositories = getPropertyList("gitlab.additional.repository")

    Logger.info("ignored repositories: " + ignored)

    val names = repositories.as[Seq[JsValue]]
      .map(repository => (repository \ "path_with_namespace").as[String])
      .filter(name => !ignored.contains(name))

    val allNames = names ++ additionalRepositories

    Logger.info("repositories: " + allNames)

    allNames

  }

  private def getProperty(property: String) = configuration.get(property)

  private def getPropertyList(property: String) = configuration.get[Seq[String]](property)

  private def getUserCredentials = {
    new UsernamePasswordCredentialsProvider(
      getProperty("gitlab.user"), getProperty("gitlab.token")
    )
  }
}
