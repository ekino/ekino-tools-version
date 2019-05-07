package utils

import java.io.File

import model.{GradlePlugin, JvmDependency, Repository, SpringBootData}
import play.api.Logger

import scala.util.matching.Regex

/**
  * Parse a gradle project and extract version data.
  */
object GradleRepositoryParser extends AbstractParser {

  val buildFileName = "build.gradle"
  val buildKotlinFileName = "build.gradle.kts"
  val propertiesFileName = "gradle.properties"
  val settingsFileName = "settings.gradle"
  val gradleWrapperFileName = "gradle/wrapper/gradle-wrapper.properties"

  val artifactRegex: Regex = ("""\s*""" +
    """(?:mavenBom|classpath|compile|compileOnly|testCompile|runtime|play|playTest|integrationTestCompile|api|implementation|testImplementation|testRuntimeOnly|runtimeOnly)""" +
    """[\s(]*""" +
    """(?:group\s*[:=]\s*)?""" +
    """\(?['"]""" +
    """(?<groupId>[_a-zA-Z0-9.-]+)""" +
    """[':"]""" +
    """(?:\s*,\s*name\s*[:=]\s*['"])?""" +
    """(?<artefactId>[_a-zA-Z0-9.-]+)""" +
    """(?:.*)(?:property\(['"]|\$\{?|:)""" +
    """(?<version>[_a-zA-Z0-9.-]+)""").r
  val propertyRegex: Regex = """([^ =\n]*) *= *([^ \n]*)""".r
  val projectNameRegex: Regex = """rootProject.name ?= ?(?:'|")([0-9a-zA-Z\-]+)""".r
  val gradleVersionRegex: Regex = """.*gradle-([0-9.-]+)-.*""".r
  val pluginRegex: Regex = (
    """\s*id\s*\(?""" +
    """['"]([_a-zA-Z0-9.-]+)['"]\)?""" +
    """\s*version\s+""" +
    """['"]([_a-zA-Z0-9.-]+)['"]""").r
  private val logger = Logger(GradleRepositoryParser.getClass)

  override def buildRepository(file: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Option[Repository] = {
    // project files
    val repositoryPath = file.getPath
    val buildFile = getBuildFile(file)
    val propertiesFile = new File(repositoryPath, propertiesFileName)
    val settingsFile = new File(repositoryPath, settingsFileName)
    val gradleVersionFile = new File(repositoryPath, gradleWrapperFileName)

    if (buildFile.exists) {

      val name = extractFromFile(settingsFile, projectNameRegex, extractValue).getOrElse("value", file.getName)
      logger.info(s"name $name")
      val extractedArtifacts = extractFromFile(buildFile, artifactRegex, extractArtifacts)
      logger.debug(s"artifacts $extractedArtifacts")
      val properties = extractFromFile(propertiesFile, propertyRegex, extractProperties)

      logger.debug(s"properties $properties")

      val gradleVersion = extractFromFile(gradleVersionFile, gradleVersionRegex, extractValue).getOrElse("value", "")
      val plugins = extractFromFile(buildFile, pluginRegex, extractProperties)
        .map(p => GradlePlugin(p._1, p._2))
        .toSeq
      val artifacts = replaceVersionsHolder(extractedArtifacts, properties)
        .map(p => JvmDependency(p._1, p._2))
        .toSeq
      val springBootData = SpringBootUtils.getSpringBootData(plugins, springBootDefaultData, springBootMasterData)
      val springBootOverrides = SpringBootUtils.getSpringBootOverrides(artifacts, properties, springBootData)
        .map(p => JvmDependency(p._1, p._2))
        .toSeq
      Some(Repository(name, groupName, artifacts ++ springBootOverrides, s"Gradle $gradleVersion", plugins))
    } else {
      // cannot process versions
      None
    }
  }

  override def getBuildFile(repositoryPath: File): File = {
    val buildFile = new File(repositoryPath, buildFileName)
    if (!buildFile.exists()) {
      new File(repositoryPath, buildKotlinFileName)
    } else {
      buildFile
    }
  }
}
