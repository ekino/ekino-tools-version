package services

import java.io.FileNotFoundException

import model.Site
import play.api.Logger
import scalaz.concurrent.Task

import scala.util.control.NonFatal
import scala.util.matching.Regex
import scala.xml.XML

/**
  * Download metadata from a maven url and return a Task of the last release.
  */
object MavenVersionFetcher extends VersionFetcher {

  val pattern: Regex = "([^:]+):(.+)".r
  private val logger = Logger(MavenVersionFetcher.getClass)

  // download maven-metadata to get the latest repository
  override def getLatestVersion(name: String, site: Site): Task[(String, String)] = Task {

    try {
      val uri = name match {
        case pattern(group, artefact) => s"${group.replace('.', '/')}/$artefact"
      }

      // maven meta data
      val url = site.url + uri + "/maven-metadata.xml"

      val xmlFromString = XML.loadString(fetchUrl(url, site))
      val version = xmlFromString \\ "release" // XPATH to select release node
      logger.info("Resolved " + url + ":" + version.text)

      (name, version.text)

    } catch {
      case _: FileNotFoundException => (name, "")
      case NonFatal(e) =>
        logger.error(s"Unexpected exception on maven fetcher for $name", e)
        (name, "")
    }
  }
}
