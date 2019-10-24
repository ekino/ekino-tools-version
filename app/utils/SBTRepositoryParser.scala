package utils

import java.io.File

import executors.pool
import model.{JvmDependency, Plugin, Repository}
import play.api.Logger
import scalaz.concurrent.Task

import scala.util.matching.Regex

/**
  * Parse a sbt project and extract version data.
  */
object SBTRepositoryParser extends AbstractParser {

  val buildFileName = "build.sbt"
  val artifactRegex: Regex = """[^"]*(?<! %% )"([^"]+)" % "([^"]+)" % "?([^ ",\n]+).*""".r
  val scalaArtifactRegex: Regex = """[^"]*"([^"]+)" %% "([^"]+)" % "?([^ ",\n]+).*""".r
  val propertyRegex: Regex = """.*val (\w+) = "([^ \n]+)"(?! %)""".r
  val scalaVersionRegex: Regex = """.*scalaVersion (?:in ThisBuild )?:= "?([^ ",\n]+).*""".r
  val scala2Regex: Regex = """(2.\d+).*""".r
  private val logger = Logger(SBTRepositoryParser.getClass)

  override def buildRepository(folder: File, groupName: String): Task[Repository] = Task {
    // project files
    val buildFile = getBuildFiles(folder).head

    val (scalaVersion, artifacts) = getDependencies(folder, buildFile)

    Repository(folder.getName, groupName, artifacts, s"SBT with scala $scalaVersion", Seq.empty[Plugin])
  }

  private def getDependencies(folder: File, buildFile: File): (String, Seq[JvmDependency]) = {
    val subfolder = getSubfolder(buildFile, folder)

    val extractedArtifacts = extractFromFile(buildFile, artifactRegex, extractArtifacts)
    val scalaArtifacts = extractFromFile(buildFile, scalaArtifactRegex, extractArtifacts)
    val properties = extractFromFile(buildFile, propertyRegex, extractProperties)
    val scalaVersion = replaceVersionsHolder(extractFromFile(buildFile, scalaVersionRegex, extractValue), properties).getOrElse("value", "2.12.0")
    val shortScalaVersion = shortenScalaVersion(scalaVersion)

    logger.debug(s"scala version for ${folder.getName} is $scalaVersion")

    val artifacts = replaceVersionsHolder(extractedArtifacts ++ appendScalaVersion(scalaArtifacts, shortScalaVersion), properties)
      .map(p => JvmDependency(p._1, p._2, subfolder))
      .toSeq
    (scalaVersion, artifacts)
  }

  override def getBuildFilesRegex: Regex = buildFileName.r

  private def appendScalaVersion(artifacts: Map[String, String], version: String): Map[String, String] = {
    artifacts
      .map(entry => (entry._1 + s"_$version", entry._2))
  }

  private def shortenScalaVersion(scalaVersion: String): String = {
    scala2Regex.findFirstMatchIn(scalaVersion).map(_.group(1)).getOrElse("2.12")
  }

}
