package services

import java.net.URL

import javax.inject.{Inject, Singleton}
import model.SpringBootData
import play.api.ConfigLoader.stringLoader
import play.api.{Configuration, Logger}
import scalaz.Memo
import scalaz.concurrent.Task

import scala.collection.immutable.ListMap
import scala.io.Source
import scala.xml.XML

/**
  * Download spring boot pom file to check if a dependency is managed by springboot.
  */
object SpringBootVersionService {

  private val propertyRegex = """\$\{(.*)\}""".r
  private val baseSpringbootPomUrl = "https://raw.githubusercontent.com/spring-projects/spring-boot/v"
  private val pomPath = "/spring-boot-dependencies/pom.xml"

  private val logger = Logger(SpringBootVersionService.getClass)

  val springBootData: String => SpringBootData = Memo.immutableHashMapMemo {
    case version: String if version.startsWith("1.") => getData(s"$baseSpringbootPomUrl$version$pomPath")
    case version: String if version.startsWith("2.") => getData(s"$baseSpringbootPomUrl$version/spring-boot-project$pomPath")
    case _                                           => SpringBootData.noData
  }

  private def getData(url: String) = {
    val connection = new URL(url).openConnection
    val html = Source.fromInputStream(connection.getInputStream)
    val xmlFromString = XML.loadString(html.mkString)
    val dependencies = xmlFromString \ "dependencyManagement" \ "dependencies" \\ "dependency" // XPATH to select dependency nodes

    val dependencyMap = dependencies
      .map(dependency => (dependency \ "groupId").text + ":" + (dependency \ "artifactId").text -> formatProperty((dependency \ "version").text))

    logger.debug(s"springboot dependencyMap: $dependencyMap")

    val properties = xmlFromString \ "properties" \ "_"
    val propertyMap = properties
      .map(property => property.label -> property.text)
      .toMap

    logger.debug(s"springboot propertyMap: $propertyMap")

    SpringBootData(ListMap(dependencyMap: _*), propertyMap)
  }

  private def formatProperty(value: String): String = {
    propertyRegex.findFirstMatchIn(value)
      .map(_.group(1))
      .getOrElse(value)
  }
}
