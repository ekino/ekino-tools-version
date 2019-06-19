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

  override def buildRepository(file: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Option[Repository] = {
    // project files
    val buildFile = getBuildFile(file)

    val source = getFileAsString(buildFile)
    val jsonValues = Json.parse(source).as[JsObject].value

    val name = jsonValues.get("name")
      .map(_.as[JsString].value)
      .getOrElse(file.getName)

    val dependencies = (extractDependencies(jsonValues.get("devDependencies")) ++ extractDependencies(jsonValues.get("dependencies")))
      .map(p => NodeDependency(p._1, p._2))
      .toSeq

    val lockFile = new File(file, lockFileName)
    val resolvedDependencies = getResolvedDependencies(lockFile)

    val result = if (resolvedDependencies.isEmpty) dependencies else resolvedDependencies.filter(d => dependencies.map(_.name).contains(d.name))

    Some(Repository(name, groupName, result, "NPM", Seq.empty[Plugin]))
  }

  private def getResolvedDependencies(lockFile: File): Seq[NodeDependency] = {
    if (lockFile.exists()) {
      Json.parse(getFileAsString(lockFile))
        .as[JsObject]
        .value
        .getOrElse("dependencies", JsObject.empty)
        .asInstanceOf[JsObject]
        .fields
        .map(p => NodeDependency(p._1, p._2.as[JsObject].value("version").as[JsString].value))
    } else {
      Seq.empty
    }
  }

  override def canProcess(repository: File): Boolean =
    super.canProcess(repository) && !YarnLockRepositoryParser.canProcess(repository)

  override def getBuildFile(repositoryPath: File): File = new File(repositoryPath, buildFileName)

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

