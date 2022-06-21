package services

import java.net.URL

import model.Site
import scalaz.concurrent.Task

import scala.io.Source

trait VersionFetcher {

  def fetchUrl(url: String, site: Site, headers: (String, String)*): String = {
    val connection = new URL(url).openConnection
    if (site.user.nonEmpty && site.password.nonEmpty) {
      connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION,
        HttpBasicAuth.getHeader(site.user, site.password)
      )
    }
    headers.foreach(header => connection.setRequestProperty(header._1, header._2))

    val html = Source.fromInputStream(connection.getInputStream)
    html.getLines().mkString
  }

  // download metadata to get the latest version
  def getLatestVersion(name: String, site: Site): Task[(String, String)]
}
