package services

import java.net.URL

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}

import scala.io.Source

@Singleton
class GitHub @Inject()(configuration: Configuration) extends AbstractGitHost("github", configuration) {

  private val linkHeaderField = "Link"
  private val lastPageLink = "rel=\"first\""

  override def getRawGroups: Option[String] =
    getProperty("github.users")

  override def getRepositories: Seq[GitRepository] = getGroups
    .flatMap(group => fetchRepositoryUrls(group, getIgnoredUrls, getProperty("github.token").getOrElse("")))

  /**
    * Fetch the repository urls recursively for the given user using GitHub API.
    */
  private def fetchRepositoryUrls(user: String, ignored: Seq[String], gitHubToken: String, page: Int = 1, accumulator: Seq[GitRepository] = Seq.empty): Seq[GitRepository] = {
    val url: String = s"https://api.github.com/users/$user/repos?access_token=$gitHubToken&per_page=100&page=$page"

    val connection = new URL(url).openConnection
    val rawRepositories = Source.fromInputStream(connection.getInputStream).mkString

    // getting the project list of github user
    val repositories = Json.parse(rawRepositories)

    val pageUrls = repositories.as[Seq[JsValue]]
      .map(repository => (repository \ "html_url").as[String])
      .filter(name => !name.contains(ignored))
      .map(repository => GitRepository(repository, getProperty("github.user").getOrElse(""), gitHubToken))

    val urls = accumulator ++ pageUrls

    val linkHeader = connection.getHeaderField(linkHeaderField)

    Logger.info(s"head : $linkHeader")

    if (linkHeader == null || linkHeader.contains(lastPageLink)) {
      Logger.info(s"github repositories: $urls")
      urls
    } else {
      fetchRepositoryUrls(user, ignored, gitHubToken, page + 1, urls)
    }
  }
}
