package utils

import java.io.{File, IOException}

import model.{Repository, SpringBootData}
import play.api.Logger

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
  private val logger = Logger(classOf[AbstractParser])

  def getBuildFile(repositoryPath: File): File

  def canProcess(repository: File): Boolean =
    getBuildFile(repository).exists()

  def buildRepository(file: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Option[Repository]

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
}
