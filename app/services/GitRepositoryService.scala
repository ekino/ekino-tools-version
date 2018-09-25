package services

import java.io.File
import java.net.URL

import javax.inject.{Inject, Singleton}
import model.CustomExecutionContext.executionContextExecutor
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import play.api.ConfigLoader.stringLoader
import play.api.libs.json.{JsValue, Json}
import play.api.{ConfigLoader, Configuration, Logger}

import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps


@Singleton
class GitRepositoryService @Inject()(configuration: Configuration) {

  private val projectUrl = """https?:\/\/[^ \/]+\/(.*)""".r

  /**
    * Update all the repositories.
    */
  def updateGitRepositories(): Unit = {
    val path = new File(getProperty("project.repositories.path"))
    if (!path.exists()) {
      // creating workspace directory
      path.mkdirs()
    }

    val eventualUnits: Seq[Future[Unit]] = fetchRepositoryNames().map(updateGitRepository)
    val sequence = Future.sequence(eventualUnits)

    // waiting for all the futures
    val timeout = configuration.get("timeout.git-update")(ConfigLoader.finiteDurationLoader)
    Await.result(sequence, timeout)
  }

  /**
    * Update a git repository.
    *
    * @param repositoryUrl the repository url
    */
  private def updateGitRepository(repositoryUrl: String): Future[Unit] = Future {

    projectUrl.findFirstMatchIn(repositoryUrl) match {
      case Some(value) => updateGitRepository(repositoryUrl, value.group(1))
      case _           => Logger.error(s"error with $repositoryUrl")
    }
  }

  private def updateGitRepository(repositoryUrl: String, repositoryName: String): Unit = {

    val repositoryDirectory = new File(getProperty("project.repositories.path"), repositoryName)

    if (repositoryDirectory.exists()) {
      pullRepository(repositoryUrl, repositoryDirectory)
    } else {
      cloneRepository(repositoryUrl, repositoryDirectory)
    }
  }

  /**
    * Update a git repository using git pull.
    *
    * @param repositoryUrl     the repository url
    * @param repositoryDirectory the repository directory
    */
  private def pullRepository(repositoryUrl: String, repositoryDirectory: File): Unit = {
    val git = new Git(new FileRepository(repositoryDirectory.getAbsolutePath + "/.git"))

    val command = git
      .pull()
      .setRebase(true)
      .setCredentialsProvider(getUserCredentials(repositoryUrl))

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
    * @param repositoryUrl       the repository url
    * @param repositoryDirectory the repository directory
    */
  private def cloneRepository(repositoryUrl: String, repositoryDirectory: File): Unit = {

    Logger.info(s"Cloning repository $repositoryUrl")

    val cloneCommand = Git.cloneRepository

    cloneCommand.setURI(repositoryUrl + ".git")

    cloneCommand.setCredentialsProvider(getUserCredentials(repositoryUrl))
    cloneCommand.setDirectory(repositoryDirectory)

    try {
      cloneCommand.call()
      Logger.info(s"$repositoryUrl repository cloned")
    } catch {
      case e: Exception => Logger.error(s"Skipping repository $repositoryUrl", e)
    }
  }

  /**
    * Fetch the repository names using gitlab api.
    *
    * @return a sequence of all the repository names
    */
  private def fetchRepositoryNames(): Seq[String] = {
    Logger.info("Fetching repositories from gitlab")

    val gitlabGroupIds = getProperty("gitlab.group-ids")

    val gitlabNames = gitlabGroupIds
      .split(',')
      .filter(_.nonEmpty)
      .flatMap(groupId => fetchGitlabRepositoryNames(groupId))

    val githubUsers = getProperty("github.users")

    val githubNames = githubUsers
      .split(',')
      .filter(_.nonEmpty)
      .flatMap(groupId => fetchGithubRepositoryNames(groupId))

    gitlabNames ++ githubNames
  }

  /**
    * Fetch the gitlab repository names for the given groupId using gitlab api.
    *
    * @param groupId The gitlab group id
    * @return a sequence of all the repository names
    */
  private def fetchGitlabRepositoryNames(groupId: String): Seq[String] = {

    // gitlab api url
    val url: String = s"${getProperty("gitlab.url")}/api/v4/groups/$groupId/projects?per_page=1000"

    val connection = new URL(url).openConnection
    connection.setRequestProperty("PRIVATE-TOKEN", getProperty("gitlab.token"))

    val result = Source.fromInputStream(connection.getInputStream).mkString

    // getting the project list of gitlab group
    val repositories = Json.parse(result)
    val ignored = getPropertyList("gitlab.ignored-repositories")

    Logger.info(s"ignored repositories: $ignored")

    val names = repositories.as[Seq[JsValue]]
      .map(repository => (repository \ "path_with_namespace").as[String])
      .filter(name => !name.contains(ignored))
      .map(getProperty("gitlab.url") + "/" + _)

    Logger.info(s"gitlab repositories: $names")

    names

  }

  /**
    * Fetch the repository names for the given user using github api.
    *
    * @param user The github user name
    * @return a sequence of all the repository names
    */
  private def fetchGithubRepositoryNames(user: String): Seq[String] = {

    // github api url
    val url: String = s"https://api.github.com/users/$user/repos?access_token${getProperty("github.token")}&per_page=100"

    val connection = new URL(url).openConnection

    val result = Source.fromInputStream(connection.getInputStream).mkString

    // getting the project list of github user
    val repositories = Json.parse(result)
    val ignored = getPropertyList("github.ignored-repositories")

    Logger.info(s"ignored repositories: $ignored")

    val names = repositories.as[Seq[JsValue]]
      .map(repository => (repository \ "html_url").as[String])
      .filter(name => !ignored.contains(name))

    Logger.info(s"github repositories: $names")

    names

  }

  private def getProperty(property: String) = configuration.get(property)

  private def getPropertyList(property: String) = configuration.get[Seq[String]](property)

  private def getUserCredentials(repository: String) = {
    if (repository.contains(getProperty("gitlab.url"))) {
      new UsernamePasswordCredentialsProvider(getProperty("gitlab.user"), getProperty("gitlab.token"))
    } else {
      new UsernamePasswordCredentialsProvider(getProperty("github.user"), getProperty("gitlab.token"))
    }
  }
}
