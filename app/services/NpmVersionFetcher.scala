package services

import java.io.FileNotFoundException
import model.Site
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, Json}
import scalaz.concurrent.Task

import scala.util.control.NonFatal

/**
  * Download npm metadata from a npm registry the last release.
  */
object NpmVersionFetcher extends VersionFetcher {

  val ACCEPT = "Accept"
  val NPM_JSON = "application/vnd.npm.install-v1+json"
  val DIST_TAGS = "dist-tags"
  val LATEST = "latest"
  private val logger = Logger(NpmVersionFetcher.getClass)


  // download npm metadata to get the latest version
  override def getLatestVersion(name: String, site: Site): Task[(String, String)] = Task {

    try {
      // npm meta data url
      val url = site.url + name

      val source = fetchUrl(url, site, (ACCEPT, NPM_JSON))
      val json = Json.parse(source).as[JsObject].value

      val version = json.get(DIST_TAGS)
        .map(_.as[JsObject].value)
        .flatMap(_.get(LATEST))
        .map(_.as[JsString].value)
        .getOrElse("")

      logger.info(s" resolved npm version $version for $name")

      (name, version)

    } catch {
      case _: FileNotFoundException => (name, "")
      case NonFatal(e) =>
        logger.error(s"Unexpected exception for $name", e)
        (name, "")
    }

  }

}
