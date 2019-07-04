package utils

import java.io.{File, IOException}
import java.nio.file.Files

import model.{Repository, SpringBootData}
import play.api.Logger

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.io.Source
import scala.util.matching.Regex
import scala.util.matching.Regex.MatchData

/**
  * Abstract class for file parsers.
  */
abstract class AbstractParser {

  // transforms a regex match data into a map entry
  type ExtractGroups = MatchData => (String, String)

  val extractProperties: ExtractGroups = matchData => matchData.group(1) -> matchData.group(2)
  val extractValue: ExtractGroups = matchData => "value" -> matchData.group(1)
  val extractArtifacts: ExtractGroups = matchData => (matchData.group(1) + ":" + matchData.group(2)).trim -> matchData.group(3)
  val excludedFolder: Regex = """.*/(?:test|.gradle|node_modules|target|build|dist)/.*""".r
  private val logger = Logger(classOf[AbstractParser])

  def getBuildFiles(repositoryPath: File): Seq[File]

  def canProcess(repository: File): Boolean =
    getBuildFiles(repository).exists(_.exists())

  def buildRepository(folder: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Repository

  // read a file and extract lines matching a pattern
  protected def extractFromFile(file: File, regex: Regex, extract: ExtractGroups): Map[String, String] = {
    try {
      // Seek all results matching regex and converting it to a map
      val content = getFileAsString(file)
      ListMap(regex.findAllIn(content)
        .matchData
        .map(extract)
        .toSeq: _*
      )
    } catch {
      case _: IOException =>
        logger.debug(s"Cannot find ${file.getName}")
        Map.empty[String, String]

    }
  }

  // replace repository holder by value
  protected def replaceVersionsHolder(artifacts: Map[String, String], properties: Map[String, String]): Map[String, String] = {
    // replace repository holders (i.e my-aterfact.version) by its value
    ListMap(artifacts
      .toSeq
      .map(p => if (properties.get(p._2).isDefined) p._1 -> properties(p._2) else p): _*)
  }

  def getFileAsString(file: File): String = {
    val sourceFile = Source.fromFile(file)
    val content = sourceFile.mkString
    sourceFile.close()
    content
  }

  protected def findBuildFilesByPattern(repositoryPath: File, regex: Regex): Seq[File] = {
    Files.walk(repositoryPath.toPath)
      .filter(Files.isRegularFile(_))
      .filter(file => regex.findFirstMatchIn(file.toString).isDefined)
      .filter(file => excludedFolder.findFirstMatchIn(file.toString).isEmpty)
      .iterator()
      .asScala
      .map(_.toFile)
      .toSeq
      .sortBy(_.toString.length())
  }

  protected def getSubfolder(buildFile: File, folder: File): String = if (buildFile.getParentFile.equals(folder)) "" else buildFile.getParentFile.getName
}
