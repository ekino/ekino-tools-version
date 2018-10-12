package services

import java.io.FileNotFoundException
import java.net.URL

import model.CustomExecutionContext.executionContextExecutor
import org.apache.commons.codec.binary.Base64
import play.api.Logger

import scala.concurrent.Future
import scala.io.Source
import scala.util.control.NonFatal
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

/**
  * Download npm metadata from a npm registry the last release.
  */
object NpmVersionFetcher {

  val ACCEPT = "Accept"
  val NPM_JSON = "application/vnd.npm.install-v1+json"
  val DIST_TAGS = "dist-tags"
  val LATEST = "latest"


  // download npm metadata to get the latest version
  def getLatestVersion(name: String, npmUrl: String): Future[(String, String)] = Future {

    try {
      // npm meta data url
      val url = npmUrl + name

      val connection = new URL(url).openConnection
      connection.setRequestProperty(ACCEPT, NPM_JSON)

      val source = Source.fromInputStream(connection.getInputStream).getLines.mkString
      val json = Json.parse(source).as[JsObject].value

      val version = json.get(DIST_TAGS)
        .map(_.as[JsObject].value)
        .flatMap(_.get(LATEST))
        .map(_.as[JsString].value)
        .getOrElse("")

      Logger.info(s" resolved npm version $version for $name")

      (name, version)

    } catch {
      case NonFatal(e) =>
        Logger.error(s"Unexpected exception for $name", e)
        (name, "")
    }

  }

}
