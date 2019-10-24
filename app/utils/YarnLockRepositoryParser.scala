package utils

import java.io.File

import model._
import scalaz.concurrent.Task
import utils.TaskHelper.gather

import scala.util.matching.Regex

/**
  * Parse a yarn project and extract version data.
  */
object YarnLockRepositoryParser extends AbstractParser {

  private val buildFileName = "yarn.lock"
  private val yarnVersion = """"?([^\n,]*)@(.*):\n\s+version "(.*)"""".r
  private val versionValue = """[.\da-zA-Z-]+""".r
  val extractYarnDependency: ExtractGroups[YarnDependency] = matchData => matchData.group(1) -> YarnDependency(matchData.group(3), matchData.group(2))

  override def buildRepository(folder: File, groupName: String): Task[Repository] = {
    // project files
    val buildFiles = getBuildFiles(folder)

    for {
      projectDependencies <- gather(buildFiles.map(getDependencies(_, folder, groupName)))
      dependencies         = projectDependencies.fold(Seq.empty[Dependency])(_ ++ _)
    } yield Repository(folder.getName, groupName, dependencies, "Yarn", Seq.empty)
  }

  private def getDependencies(buildFile: File, folder: File, groupName: String): Task[Seq[Dependency]] = {
    val subfolder = getSubfolder(buildFile, folder)

    for {
      repository <- NPMRepositoryParser.buildRepository(buildFile.getParentFile, groupName)
      npmArtifacts = repository.dependencies
    } yield extractFromFile(buildFile, yarnVersion, extractYarnDependency)
      .filter(p => npmArtifacts.exists(dep => dep.name == p._1 && containsVersion(p._2.semver, dep.version)))
      .map(p => NodeDependency(p._1, p._2.version, subfolder))
      .toSeq
  }

  override def getBuildFilesRegex: Regex = buildFileName.r

  def containsVersion(line: String, version: String): Boolean =
    versionValue.findAllIn(line)
      .matchData
      .map(_.matched)
      .contains(version)
}

case class YarnDependency(version: String, semver: String)

