package utils

import java.io.File

import model._

import scala.util.matching.Regex

/**
  * Parse a yarn project and extract version data.
  */
object YarnLockRepositoryParser extends AbstractParser {

  private val buildFileName = "yarn.lock"
  private val yarnVersion = """"?([^\n,]*)@.*:\n\s+version "(.*)"""".r

  override def buildRepository(folder: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Repository = {
    // project files
    val buildFiles = getBuildFiles(folder)

    val dependencies = buildFiles
      .map(getDependencies(_, folder, groupName))
      .fold(Seq.empty[Dependency])((d1, d2) => d1 ++ d2)

    Repository(folder.getName, groupName, dependencies, "Yarn", Seq.empty)
  }

  private def getDependencies(buildFile: File, folder: File, groupName: String): Seq[Dependency] = {
    val subfolder = getSubfolder(buildFile, folder)

    val npmArtifacts = NPMRepositoryParser.buildRepository(buildFile.getParentFile, groupName, SpringBootData.noData, SpringBootData.noData)
      .dependencies
      .map(_.name)

    extractFromFile(buildFile, yarnVersion, extractProperties)
      .filter(p => npmArtifacts.contains(p._1))
      .map(p => NodeDependency(p._1, p._2, subfolder))
      .toSeq
  }

  override def getBuildFilesRegex: Regex = buildFileName.r
}

