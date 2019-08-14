package services

import java.net.URL

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}

import scala.annotation.tailrec
import scala.io.Source

@Singleton
class GitLab @Inject()(configuration: Configuration) extends AbstractGitHost("gitlab", configuration) {

  private val nextPageHeader = "X-Next-Page"
  private val logger = Logger(classOf[GitLab])

  override def getRawGroups: Option[String] =
    getProperty("gitlab.group-ids")

  override def getRepositories: Seq[GitRepository] = {
    val repositories = getGroups
      .flatMap(group => fetchGitlabUrls(group))
    val additionalRepositories = getAdditionalUrls.map(getGitRepository)
    repositories ++ additionalRepositories
  }

  /**
    * Fetch the repository urls recursively for the given group ID using GitLab API.
    */
  @tailrec
  private def fetchGitlabUrls(groupId: String, page: Int = 1, accumulator: Seq[GitRepository] = Seq.empty): Seq[GitRepository] = {
    val url: String = s"$gitlabUrl/api/v4/groups/$groupId/projects?per_page=100&page=$page"

    val connection = new URL(url).openConnection
    connection.setRequestProperty("PRIVATE-TOKEN", gitlabToken)

    val rawRepositories = Source.fromInputStream(connection.getInputStream).mkString

    // getting the project list of gitlab group
    val repositories = Json.parse(rawRepositories)

    val ignored = getIgnoredUrls
    val pageUrls = repositories.as[Seq[JsValue]]
      .map(repository => (repository \ "path_with_namespace").as[String])
      .filter(name => !ignored.contains(name))
      .map(getGitRepository)

    val urls = accumulator ++ pageUrls

    if (connection.getHeaderField(nextPageHeader).isEmpty) {
      logger.info(s"gitlab repositories: $urls")
      urls
    } else {
      fetchGitlabUrls(groupId, page + 1, urls)
    }
  }

  private def getGitRepository(repository: String): GitRepository = {
    GitRepository(gitlabUrl + "/" + repository, getProperty("gitlab.user").getOrElse(""), gitlabToken)
  }

  private def gitlabUrl: String = getProperty("gitlab.url").getOrElse("")

  private def gitlabToken: String = getProperty("gitlab.token").getOrElse("")
}
