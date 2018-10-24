package services

import java.net.URL

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}

import scala.io.Source

@Singleton
class GitLab @Inject()(configuration: Configuration) extends AbstractGitHost("gitlab", configuration) {

  private val nextPageHeader = "X-Next-Page"

  override def getRawGroups: Option[String] =
    getProperty("gitlab.group-ids")

  override def getRepositories: Seq[GitRepository] = getGroups
    .flatMap(group => fetchGitlabUrls(getProperty("gitlab.url").getOrElse(""), getProperty("gitlab.token").getOrElse(""), group, getIgnoredUrls))

  /**
    * Fetch the repository urls recursively for the given group ID using GitLab API.
    */
  private def fetchGitlabUrls(gitlabUrl: String, gitlabToken: String, groupId: String, ignored: Seq[String], page: Int = 1, accumulator: Seq[GitRepository] = Seq.empty): Seq[GitRepository] = {
    val url: String = s"$gitlabUrl/api/v4/groups/$groupId/projects?per_page=100&page=$page"

    val connection = new URL(url).openConnection
    connection.setRequestProperty("PRIVATE-TOKEN", gitlabToken)

    val rawRepositories = Source.fromInputStream(connection.getInputStream).mkString

    // getting the project list of gitlab group
    val repositories = Json.parse(rawRepositories)

    val pageUrls = repositories.as[Seq[JsValue]]
      .map(repository => (repository \ "path_with_namespace").as[String])
      .filter(name => !name.contains(ignored))
      .map(gitlabUrl + "/" + _)
      .map(repository => GitRepository(repository, getProperty("gitlab.user").getOrElse(""), gitlabToken))

    val urls = accumulator ++ pageUrls

    if (connection.getHeaderField(nextPageHeader).isEmpty) {
      Logger.info(s"gitlab repositories: $urls")
      urls
    } else {
      fetchGitlabUrls(gitlabUrl, gitlabToken, groupId, ignored, page + 1, urls)
    }
  }
}
