package services

import java.net.URL

import javax.inject.Singleton
import org.apache.commons.codec.binary.Base64
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.matching.Regex

/**
  * Download metadata from a maven url and return a Future of the last release.
  */
@Singleton
class MavenVersionFetcher {

  val pattern: Regex = """([^:]+):(.+)""".r

  // download maven-metadata to get the latest repository
  def getLatestVersion(name: String, mavenUrl: String, mavenUser: String, mavenPassword: String): Future[(String, String)] = Future {

    var uri: String = null
    try {
      pattern.findAllIn(name).matchData foreach {
        matchData => {
          uri = matchData.group(1).replace('.', '/') + "/" + matchData.group(2)
        }
      }

      // maven meta data
      val url = mavenUrl + uri + "/maven-metadata.xml"

      val connection = new URL(url).openConnection
      if (!mavenUser.isEmpty && !mavenPassword.isEmpty) {
        connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION,
          HttpBasicAuth.getHeader(mavenUser, mavenPassword)
        )
      }

      val html = Source.fromInputStream(connection.getInputStream)
      val xmlFromString = scala.xml.XML.loadString(html.mkString)
      val version = xmlFromString \\ "release" // XPATH to select release node
      Logger.info("Resolved " + url + ":" + version.text)

      (name, version.text)

    } catch {
      case _: java.io.FileNotFoundException => (name, "")
      case e: Exception => {
        Logger.error(s"Unexpected exception for $name", e)
        (name, "")
      }
    }

  }

  object HttpBasicAuth {
    val BASIC = "Basic"
    val AUTHORIZATION = "Authorization"

    def getHeader(username: String, password: String): String =
      BASIC + " " + encodeCredentials(username, password)

    def encodeCredentials(username: String, password: String): String =
      new String(Base64.encodeBase64String((username + ":" + password).getBytes))
  }

}
