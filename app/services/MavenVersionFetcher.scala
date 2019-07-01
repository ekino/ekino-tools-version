package services

import java.io.FileNotFoundException
import java.net.URL

import model.CustomExecutionContext.executionContextExecutor
import model.Site
import play.api.Logger

import scala.concurrent.Future
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.matching.Regex
import scala.xml.XML

/**
  * Download metadata from a maven url and return a Future of the last release.
  */
object MavenVersionFetcher {

  val pattern: Regex = "([^:]+):(.+)".r
  private val logger = Logger(MavenVersionFetcher.getClass)

  // download maven-metadata to get the latest repository
  def getLatestVersion(name: String, site: Site): Future[(String, String)] = Future {

    try {
      val uri = name match {
        case pattern(group, artefact) => s"${group.replace('.', '/')}/$artefact"
      }

      // maven meta data
      val url = site.url + uri + "/maven-metadata.xml"

      val connection = new URL(url).openConnection
      if (!site.user.isEmpty && !site.password.isEmpty) {
        connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION,
          HttpBasicAuth.getHeader(site.user, site.password)
        )
      }

      val html = Source.fromInputStream(connection.getInputStream)
      val xmlFromString = XML.loadString(html.mkString)
      val version = xmlFromString \\ "release" // XPATH to select release node
      logger.info("Resolved " + url + ":" + version.text)

      (name, version.text)

    } catch {
      case _: FileNotFoundException => (name, "")
      case NonFatal(e) =>
        logger.error(s"Unexpected exception for $name", e)
        (name, "")
    }
  }
}
