package services

import java.io.File
import java.net.URL

import javax.inject.{Inject, Singleton}
import model.CustomExecutionContext.executionContextExecutor
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import play.api.libs.json.{JsValue, Json}
import play.api.{ConfigLoader, Configuration, Logger}

import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps
import GitRepositoryService._
import scala.util.Try

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

    val updatedRepositories: Seq[Future[Unit]] = fetchRepositoryUrls().map(updateGitRepository)
    val sequence = Future.sequence(updatedRepositories)

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

  private def updateGitRepository(repositoryUrl: String, repositoryName: String): Unit = Try {

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
    * Fetch the repository urls using gitlab or github api.
    *
    * @return a sequence of all the repository urls
    */
  private def fetchRepositoryUrls(): Seq[String] = {
    Logger.info("Fetching repositories from gitlab")

    val gitlabGroupIds = getProperty("gitlab.group-ids")

    val gitlabIgnored = getPropertyList("gitlab.ignored-repositories")
    Logger.info(s"gitlab ignored repositories: $gitlabIgnored")

    val gitlabUrls = gitlabGroupIds
      .split(',')
      .filter(_.nonEmpty)
      .flatMap(groupId => fetchGitlabRepositoryUrls(groupId, gitlabIgnored))

    val githubUsers = getProperty("github.users")

    val githubIgnored = getPropertyList("github.ignored-repositories")
    Logger.info(s"github ignored repositories: $githubIgnored")

    val githubUrls = githubUsers
      .split(',')
      .filter(_.nonEmpty)
      .flatMap(user => fetchGithubRepositoryUrls(user, githubIgnored))

    gitlabUrls ++ githubUrls
  }

  /**
    * Fetch the gitlab repository urls recursively for the given groupId using gitlab api.
    *
    * @param groupId      the gitlab group id
    * @param ignored      the ignored repositories
    * @param page         first page to fetch (default 1)
    * @param accumulator  already retreived repository urls
    * @return a sequence of all the repository urls
    */
  private def fetchGitlabRepositoryUrls(groupId: String, ignored: Seq[String], page: Int = 1, accumulator: Seq[String] = Seq.empty): Seq[String] = {
    val url: String = s"${getProperty("gitlab.url")}/api/v4/groups/$groupId/projects?per_page=100&page=$page"

    val connection = new URL(url).openConnection
    connection.setRequestProperty("PRIVATE-TOKEN", getProperty("gitlab.token"))

    val result = Source.fromInputStream(connection.getInputStream).mkString

    // getting the project list of gitlab group
    val repositories = Json.parse(result)

    val pageUrls = repositories.as[Seq[JsValue]]
      .map(repository => (repository \ "path_with_namespace").as[String])
      .filter(name => !name.contains(ignored))
      .map(getProperty("gitlab.url") + "/" + _)

    val urls = accumulator ++ pageUrls

    if (connection.getHeaderField(gitlabNextPageHeader).isEmpty) {
      Logger.info(s"gitlab repositories: $urls")
      urls
    } else {
      fetchGitlabRepositoryUrls(groupId, ignored, page + 1, urls)
    }
  }

  /**
    * Fetch the repository urls recursively for the given user using github api.
    *
    * @param user         the github user name
    * @param ignored      the ignored repositories
    * @param page         first page to fetch (default 1)
    * @param accumulator  already retreived repository urls
    * @return a sequence of all the repository urls
    */
  private def fetchGithubRepositoryUrls(user: String, ignored: Seq[String], page: Int = 1, accumulator: Seq[String] = Seq.empty): Seq[String] = {

    // github api url
    val url: String = s"https://api.github.com/users/$user/repos?access_token=${getProperty("github.token")}&per_page=100&page=$page"

    val connection = new URL(url).openConnection

    val result = Source.fromInputStream(connection.getInputStream).mkString

    // getting the project list of github user
    val repositories = Json.parse(result)

    val pageUrls = repositories.as[Seq[JsValue]]
      .map(repository => (repository \ "html_url").as[String])
      .filter(name => !name.contains(ignored))

    val urls = accumulator ++ pageUrls

    val linkHeader = connection.getHeaderField(githubLinkHeader)

    if (linkHeader == null || linkHeader.contains(githubLastPageLink)) {
      Logger.info(s"github repositories: $urls")
      urls
    } else {
      fetchGithubRepositoryUrls(user, ignored, page + 1, urls)
    }
  }

  private def getProperty(property: String): String = configuration.getOptional[String](property).getOrElse("")

  private def getPropertyList(property: String) = configuration.get[Seq[String]](property)

  private def getUserCredentials(repository: String) = {
    if (repository.contains(getProperty("gitlab.url"))) {
      new UsernamePasswordCredentialsProvider(getProperty("gitlab.user"), getProperty("gitlab.token"))
    } else {
      new UsernamePasswordCredentialsProvider(getProperty("github.user"), getProperty("gitlab.token"))
    }
  }
}

object GitRepositoryService {
  private val gitlabNextPageHeader = "X-Next-Page"
  private val githubLinkHeader = "Link"
  private val githubLastPageLink = "rel=\"first\""
}
