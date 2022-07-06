package services

import java.net.URL
import javax.inject.{Inject, Singleton}
import model.SpringBootData
import play.api.ConfigLoader.stringLoader
import play.api.{Configuration, Logger}
import scalaz.Memo
import scalaz.concurrent.Task

import scala.:+
import scala.annotation.tailrec
import scala.collection.immutable.ListMap
import scala.io.Source
import scala.xml.XML

/**
  * Download spring boot pom file to check if a dependency is managed by springboot.
  */
object SpringBootVersionService {

  private val propertyRegex = """\$\{(.*)}""".r
  private val libraryVersion = """\s+library\("[^"]+", "([^"]+)".*""".r
  private val groupName = """\s+group\("([^"]+)".*""".r
  private val artifactName = """\s+"([^"]+)".*""".r
  private val endBlock = """\t}""".r
  private val innerEndBlock = """\t\t}""".r

  private val baseSpringbootUrl = "https://raw.githubusercontent.com/spring-projects/spring-boot/v"
  private val pomPath = "/spring-boot-dependencies/pom.xml"
  private val gradlePath = "/spring-boot-dependencies/build.gradle"
  private val springboot2VersionWithPom = List("2.0", "2.1", "2.2")

  private val logger = Logger(SpringBootVersionService.getClass)

  val springBootData: String => SpringBootData = Memo.immutableHashMapMemo {
    case version if version.startsWith("1.")                             => getData(s"$baseSpringbootUrl$version$pomPath")
    case version if springboot2VersionWithPom.exists(version.startsWith) => getData(s"$baseSpringbootUrl$version/spring-boot-project$pomPath")
    case version                                                         => parseData(s"$baseSpringbootUrl$version/spring-boot-project$gradlePath")
  }

  private def formatProperty(value: String): String = {
    propertyRegex.findFirstMatchIn(value)
      .map(_.group(1))
      .getOrElse(value)
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

  private def parseData(url: String) = {
    val connection = new URL(url).openConnection
    val groovy = Source.fromInputStream(connection.getInputStream).mkString
    val librairies = parseLibrairies(groovy.split('\n').toList, List.empty)

    val artifacts = librairies
      .flatMap(l => l.groups.map(g => g.modules.map(m => (g.groupName + ":" + m.name, l.version))))
      .flatten
      .toMap

    SpringBootData(artifacts, Map.empty)
  }

  @tailrec
  private def parseLibrairies(lines: List[String], librairies: List[Library], current: TextParsing = NoText): List[Library] = {
    lines match {
      case libraryVersion(version)::tail              => parseLibrairies(tail, librairies, TextParsing(version, List.empty))
      case endBlock()::tail if current != NoText      => parseLibrairies(tail, librairies :+ Library(current.id, parseGroups(current.lines)))
      case head::tail if current != NoText            => parseLibrairies(tail, librairies, TextParsing(current.id, current.lines :+ head))
      case _::tail                                    => parseLibrairies(tail, librairies)
      case _                                          => librairies
    }
  }

  @tailrec
  private def parseGroups(lines: List[String], groups: List[Group] = List.empty, current: TextParsing = NoText): List[Group] = {
    lines match {
      case groupName(name)::tail                      => parseGroups(tail, groups, TextParsing(name, List.empty))
      case innerEndBlock()::tail if current != NoText => parseGroups(tail, groups :+ Group(current.id, parseModules(current.lines)))
      case head::tail if current != NoText            => parseGroups(tail, groups, TextParsing(current.id, current.lines :+ head))
      case _::tail                                    => parseGroups(tail, groups)
      case _                                          => groups
    }
  }

  @tailrec
  private def parseModules(lines: List[String], modules: List[Module] = List.empty): List[Module] = {
    lines match {
      case artifactName(artifact)::tail               => parseModules(tail, modules :+ Module(artifact))
      case _::tail                                    => parseModules(tail, modules)
      case _                                          => modules
    }
  }
}

private case class TextParsing(id: String, lines: List[String] = List.empty)
private object NoText extends TextParsing("")
private case class Library(version: String, groups: List[Group])
private case class Group(groupName: String, modules: List[Module])
private case class Module(name: String)
