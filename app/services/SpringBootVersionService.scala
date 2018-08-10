package services

import java.net.URL

import javax.inject.{Inject, Singleton}
import model.SpringBootData
import play.api.ConfigLoader.stringLoader
import play.api.{Configuration, Logger}

import scala.collection.immutable.ListMap
import scala.io.Source
import scala.xml.XML

/**
  * Download spring boot pom file to check if a dependency is managed by springboot.
  */
@Singleton
class SpringBootVersionService @Inject()(configuration: Configuration) {

  private val propertyRegex = """\$\{(.*)\}""".r

  def computeSpringBootData(master: Boolean): SpringBootData = {
    if (master) {
      getData(configuration.get("github.springboot.master.url"))
    } else {
      getData(configuration.get("github.springboot.url"))
    }
  }

  private def getData(url: String) = {
    val connection = new URL(url).openConnection
    val html = Source.fromInputStream(connection.getInputStream)
    val xmlFromString = XML.loadString(html.mkString)
    val dependencies = xmlFromString \ "dependencyManagement" \ "dependencies" \\ "dependency" // XPATH to select dependency nodes

    val dependencyMap = dependencies
      .map(dependency => (dependency \ "groupId").text + ":" + (dependency \ "artifactId").text -> formatProperty((dependency \ "version").text))

    Logger.debug(s"springboot dependencyMap: $dependencyMap")

    val properties = xmlFromString \ "properties" \ "_"
    val propertyMap = properties
      .map(property => property.label -> property.text)
      .toMap

    Logger.debug(s"springboot propertyMap: $propertyMap")

    SpringBootData(ListMap(dependencyMap: _*), propertyMap)
  }

  private def formatProperty(value: String): String = {
    propertyRegex.findFirstMatchIn(value)
      .map(_.group(1))
      .getOrElse(value)
  }
}
