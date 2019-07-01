package utils

import java.io.File

import model._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

/**
  * Parse a npm project and extract version data.
  */
object NPMRepositoryParser extends AbstractParser {

  private val buildFileName = "package.json"
  private val lockFileName = "package-lock.json"
  private val npmVersion = """[~<>=v\^]*(.*)""".r

  override def buildRepository(folder: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Repository = {
    // project files
    val buildFiles = getBuildFiles(folder)

    val dependencies = buildFiles.map(getDependencies(_, folder)).reduce((d1, d2) => d1 ++ d2)

    Repository(folder.getName, groupName, dependencies, "NPM", Seq.empty[Plugin])
  }

  private def getDependencies(buildFile: File, folder: File): Seq[Dependency] = {
    val subfolder = getSubfolder(buildFile, folder)

    val source = getFileAsString(buildFile)
    val jsonValues = Json.parse(source).as[JsObject].value

    val dependencies = (extractDependencies(jsonValues.get("devDependencies")) ++ extractDependencies(jsonValues.get("dependencies")))
      .map(p => NodeDependency(p._1, p._2, subfolder))
      .toSeq

    val lockFile = new File(buildFile.getParentFile, lockFileName)
    val resolvedDependencies = getResolvedDependencies(lockFile, subfolder)

    if (resolvedDependencies.isEmpty) dependencies else resolvedDependencies.filter(d => dependencies.map(_.name).contains(d.name))
  }

  private def getResolvedDependencies(lockFile: File, subfolder: String): Seq[NodeDependency] = {
    if (lockFile.exists()) {
      Json.parse(getFileAsString(lockFile))
        .as[JsObject]
        .value
        .getOrElse("dependencies", JsObject.empty)
        .asInstanceOf[JsObject]
        .fields
        .map(p => NodeDependency(p._1, p._2.as[JsObject].value("version").as[JsString].value, subfolder))
    } else {
      Seq.empty
    }
  }

  override def canProcess(repository: File): Boolean =
    super.canProcess(repository) && !YarnLockRepositoryParser.canProcess(repository)

  override def getBuildFiles(repositoryPath: File): Seq[File] = findBuildFilesByPattern(repositoryPath, buildFileName.r)

  private def extractDependencies(value: Option[JsValue]): Map[String, String] =
    value
      .map(_.as[JsObject]
        .value
        .mapValues(v => trimVersion(v.as[JsString].value)))
      .getOrElse(Map.empty)
      .toMap

  private def trimVersion(version: String) =
    version match {
      case npmVersion(v) => v
    }
}

