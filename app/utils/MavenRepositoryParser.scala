package utils

import java.io.File

import model._

import scala.util.matching.Regex

/**
  * Parse a gradle project and extract version data.
  */
object MavenRepositoryParser extends AbstractParser {

  val buildFileName = "pom.xml"
  val mavenWrapperFileName = ".mvn/wrapper/maven-wrapper.properties"

  val artifactRegex: Regex = """\s*<dependency>\n\s*<groupId>([a-zA-Z0-9.-]+)<\/groupId>\n\s*<artifactId>([a-zA-Z0-9.-]+)<\/artifactId>\n\s*<version>(?:\$\{)?([a-zA-Z0-9.-]+)(\})?<\/version>""".r
  val propertyRegex: Regex = """<([a-zA-Z0-9.-]+\.version)>([a-zA-Z0-9.-]+)<\/[a-zA-Z0-9.-]+\.version>""".r
  val projectNameRegex: Regex = """\s*<groupId>[a-zA-Z0-9.-]+<\/groupId>\n\s*<artifactId>([a-zA-Z0-9.-]+)<\/artifactId>\n\s*<version>[a-zA-Z0-9.-]+<\/version>\n\s*<packaging>""".r
  val mavenVersionRegex: Regex = """.*apache-maven-([0-9.-]+)-.*""".r
  val pluginRegex: Regex = """\s*<plugin>\n\s*<groupId>([a-zA-Z0-9.-]+)<\/groupId>\n\s*<artifactId>([a-zA-Z0-9.-]+)<\/artifactId>\n\s*<version>(?:\$\{)?([a-zA-Z0-9.-]+)(\})?<\/version>""".r

  override def buildRepository(file: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Option[Repository] = {
    // project files
    val repositoryPath = file.getPath
    val buildFile = getBuildFile(file)

    val name = extractFromFile(buildFile, projectNameRegex, extractValue).getOrElse("value", file.getName)
    val extractedArtifacts = extractFromFile(buildFile, artifactRegex, extractArtifacts)

    val properties = extractFromFile(buildFile, propertyRegex, extractProperties)
    val mavenVersion = extractFromFile(new File(repositoryPath, mavenWrapperFileName), mavenVersionRegex, extractValue).getOrElse("value", "")
    val extractedPlugins = extractFromFile(buildFile, pluginRegex, extractArtifacts)
    val artifacts = replaceVersionsHolder(extractedArtifacts, properties)
      .map(p => JvmDependency(p._1, p._2))
      .toSeq
    val plugins = replaceVersionsHolder(extractedPlugins, properties)
      .map(p => MavenPlugin(p._1, p._2))
      .toSeq

    val springBootData = SpringBootUtils.getSpringBootData(plugins, springBootDefaultData, springBootMasterData)
    val springBootOverrides = SpringBootUtils.getSpringBootOverrides(artifacts, properties, springBootData)
      .map(p => JvmDependency(p._1, p._2))
      .toSeq

    Some(Repository(name, groupName, artifacts ++ springBootOverrides, s"Maven $mavenVersion", plugins))
  }

  override def getBuildFile(repositoryPath: File): File = {
    new File(repositoryPath, buildFileName)
  }

}

