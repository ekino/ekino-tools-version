package utils

import java.io.File

import executors.pool
import model._
import play.api.Logger
import scalaz.concurrent.Task

import scala.util.matching.Regex

/**
  * Parse a gradle project and extract version data.
  */
object GradleRepositoryParser extends AbstractParser {

  val buildFileRegex: Regex = "build.gradle(?:\\.kts)?".r
  val propertiesFileName = "gradle.properties"
  val settingsFileName = "settings.gradle"
  val gradleWrapperFileName = "gradle/wrapper/gradle-wrapper.properties"

  val artifactRegex: Regex = ("""\s*""" +
    """(?:mavenBom|classpath|api|(?:i|testI|integrationTestI)mplementation|(?:c|testC|integrationTestC)ompile(?:Only)?|(?:r|testR|integrationTestR)untime(?:Only)?|(?:a|testA|integrationTestA)nnotationProcessor|play(?:Test)?)""" +
    """[\s(]*""" +
    """(?:(?:enforcedP|p)latform\()?""" +
    """(?:group\s*[:=]\s*)?""" +
    """\(?['"]""" +
    """(?<groupId>[_a-zA-Z0-9.-]+)""" +
    """[':"]""" +
    """(?:\s*,\s*name\s*[:=]\s*['"])?""" +
    """(?<artefactId>[_a-zA-Z0-9.-]+)""" +
    """(?:.*)(?:property\(['"]|\$\{?|:)""" +
    """(?<version>[_a-zA-Z0-9.-]+)""").r
  val propertyRegex: Regex = """([^ =\n]*) *= *"?([^ \n"]*)""".r
  val projectNameRegex: Regex = """rootProject.name ?= ?['"]([0-9a-zA-Z\-]+)""".r
  val gradleVersionRegex: Regex = """.*gradle-([0-9.-]+)-.*""".r
  val pluginRegex: Regex = (
    """\s*id\s*\(?""" +
    """['"]([_a-zA-Z0-9.-]+)['"]\)?""" +
    """\s*version\s+""" +
    """['"]?([_a-zA-Z0-9.-]+)['"]?""").r
  private val logger = Logger(GradleRepositoryParser.getClass)

  override def buildRepository(folder: File, groupName: String): Task[Repository] = Task {
    // project files
    val repositoryPath = folder.getPath
    val buildFiles = getBuildFiles(folder)
    val propertiesFile = new File(repositoryPath, propertiesFileName)
    val settingsFile = new File(repositoryPath, settingsFileName)
    val gradleVersionFile = new File(repositoryPath, gradleWrapperFileName)
    val defaultProperties = extractFromFile(propertiesFile, propertyRegex, extractProperties)

    val name = extractFromFile(settingsFile, projectNameRegex, extractValue).getOrElse("value", folder.getName)
    logger.info(s"name $name")
    val gradleVersion = extractFromFile(gradleVersionFile, gradleVersionRegex, extractValue).getOrElse("value", "")

    val dependencies = buildFiles
      .map(getDependencies(_, folder, defaultProperties))
      .reduce((r1, r2) => (r1._1 ++ r2._1, r1._2 ++ r2._2))

    Repository(name, groupName, dependencies._1, s"Gradle $gradleVersion", dependencies._2)
  }

  private def getDependencies(buildFile: File, folder: File, defaultProperties: Map[String, String]): (Seq[Dependency], Seq[Plugin]) = {
    val subfolder = getSubfolder(buildFile, folder)

    val extractedArtifacts = extractFromFile(buildFile, artifactRegex, extractArtifacts)
    logger.debug(s"artifacts $extractedArtifacts")

    val otherProperties = extractFromFile(buildFile, propertyRegex, extractProperties)
    val properties = defaultProperties ++ otherProperties

    logger.debug(s"properties $properties")

    val plugins = replaceVersionsHolder(extractFromFile(buildFile, pluginRegex, extractProperties), properties)
      .map(p => GradlePlugin(p._1, p._2))
      .toSeq
    val artifacts = replaceVersionsHolder(extractedArtifacts, properties)
      .map(p => JvmDependency(p._1, p._2, subfolder))
      .toSeq
    val springBootData = SpringBootUtils.getSpringBootData(plugins)
    val springBootOverrides = SpringBootUtils.getSpringBootOverrides(artifacts, properties, springBootData)
      .map(p => JvmDependency(p._1, p._2, subfolder))
      .toSeq
    (artifacts ++ springBootOverrides, plugins)
  }

  override def getBuildFilesRegex: Regex = buildFileRegex
}
