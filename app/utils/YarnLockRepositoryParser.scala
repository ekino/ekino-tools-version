package utils

import java.io.File

import model._

/**
  * Parse a yarn project and extract version data.
  */
object YarnLockRepositoryParser extends AbstractParser {

  private val buildFileName = "yarn.lock"
  private val yarnVersion = """"?([^\n,]*)\@.*:\n\s+version "(.*)"""".r

  override def buildRepository(file: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Option[Repository] = {
    // project files
    val buildFile = getBuildFile(file)

    val npmArtifacts = NPMRepositoryParser.buildRepository(file, groupName, springBootDefaultData, springBootMasterData)
      .get
      .dependencies
      .map(_.name)

    val extractedArtifacts = extractFromFile(buildFile, yarnVersion, extractProperties)
      .filter(p => npmArtifacts.contains(p._1))
      .map(p => NodeDependency(p._1, p._2))
      .toSeq

    Some(Repository(file.getName, groupName, extractedArtifacts, "Yarn", Seq.empty))
  }

  override def getBuildFile(repositoryPath: File): File = new File(repositoryPath, buildFileName)

}

