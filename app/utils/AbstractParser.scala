package utils

import java.io.{File, FileNotFoundException, IOException}
import java.nio.file.Files

import model.Repository
import play.api.Logger
import scalaz.concurrent.Task

import scala.jdk.CollectionConverters._
import scala.collection.immutable.ListMap
import scala.io.Source
import scala.util.matching.Regex
import scala.util.matching.Regex.MatchData

/**
  * Abstract class for file parsers.
  */
abstract class AbstractParser {

  // transforms a regex match data into a map entry
  type ExtractGroups[T] = MatchData => (String, T)

  val extractProperties: ExtractGroups[String] = matchData => matchData.group(1) -> matchData.group(2)
  val extractValue: ExtractGroups[String] = matchData => "value" -> matchData.group(1)
  val extractArtifacts: ExtractGroups[String] = matchData => (matchData.group(1) + ":" + matchData.group(2)).trim -> matchData.group(3)
  val excludedFolder: Regex = """.*/(?:test|.gradle|node_modules|target|build|dist)/.*""".r
  private val logger = Logger(classOf[AbstractParser])

  def getBuildFilesRegex: Regex

  protected def getBuildFiles(repositoryPath: File): Seq[File] = findBuildFilesByPattern(repositoryPath, getBuildFilesRegex)

  def canProcess(repository: File): Boolean =
    getBuildFiles(repository).exists(_.exists())

  def buildRepository(folder: File, groupName: String): Task[Repository]

  // read a file and extract lines matching a pattern
  protected def extractFromFile[T](file: File, regex: Regex, extract: ExtractGroups[T]): Map[String, T] = {
    try {
      // Seek all results matching regex and converting it to a map
      val content = getFileAsString(file)
      ListMap(regex.findAllIn(content)
        .matchData
        .map(extract)
        .toSeq: _*
      )
    } catch {
      case _: FileNotFoundException =>
        logger.debug(s"Cannot find ${file.getName}")
        Map.empty[String, T]
      case e: IOException =>
        logger.warn(s"Cannot read ${file.getName}", e)
        Map.empty[String, T]
    }
  }

  // replace repository holder by value
  protected def replaceVersionsHolder(artifacts: Map[String, String], properties: Map[String, String]): Map[String, String] = {
    // replace repository holders (i.e my-artifact.version) by its value
    ListMap(artifacts
      .toSeq
      .map(p => if (properties.contains(p._2)) p._1 -> properties(p._2) else p): _*)
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

  protected def getSubfolder(buildFile: File, folder: File): String = {
    val path = folder.getPath
    val parentPath = buildFile.getParentFile.getPath
    if (path.equals(parentPath)) ""
    else if (path.startsWith(parentPath)) path.substring(parentPath.length + 1)
    else buildFile.getParentFile.getName
  }
}
