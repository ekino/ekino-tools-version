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

  override def buildRepository(folder: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Repository = {
    // project files
    val repositoryPath = folder.getPath
    val buildFiles = getBuildFiles(folder)

    val defaultProperties = extractFromFile(buildFiles.head, propertyRegex, extractProperties)
    val mavenVersion = extractFromFile(new File(repositoryPath, mavenWrapperFileName), mavenVersionRegex, extractValue).getOrElse("value", "")

    val dependencies = buildFiles
      .map(getDependencies(_, folder, springBootDefaultData, springBootMasterData, defaultProperties))
      .reduce((r1, r2) => (r1._1 ++ r2._1, r1._2 ++ r2._2))

    Repository(folder.getName, groupName, dependencies._1, s"Maven $mavenVersion", dependencies._2)
  }

  private def getDependencies(buildFile: File, folder: File, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData, defaultProperties: Map[String, String]): (Seq[Dependency], Seq[Plugin]) = {
    val subfolder = getSubfolder(buildFile, folder)
    val otherProperties = extractFromFile(buildFile, propertyRegex, extractProperties)
    val properties = defaultProperties ++ otherProperties

    val extractedArtifacts = extractFromFile(buildFile, artifactRegex, extractArtifacts)
    val extractedPlugins = extractFromFile(buildFile, pluginRegex, extractArtifacts)
    val artifacts = replaceVersionsHolder(extractedArtifacts, properties)
      .map(p => JvmDependency(p._1, p._2, subfolder))
      .toSeq
    val plugins = replaceVersionsHolder(extractedPlugins, properties)
      .map(p => MavenPlugin(p._1, p._2))
      .toSeq

    val springBootData = SpringBootUtils.getSpringBootData(plugins, springBootDefaultData, springBootMasterData)
    val springBootOverrides = SpringBootUtils.getSpringBootOverrides(artifacts, properties, springBootData)
      .map(p => JvmDependency(p._1, p._2, subfolder))
      .toSeq

    (artifacts ++ springBootOverrides, plugins)
  }

  override def getBuildFiles(repositoryPath: File): Seq[File] = findBuildFilesByPattern(repositoryPath, buildFileName.r)
}

