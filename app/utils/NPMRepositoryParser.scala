package utils

import java.io.File

import model.{Repository, SpringBootData}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

import scala.io.Source

/**
  * Parse a npm project and extract version data.
  */
object NPMRepositoryParser extends AbstractParser {

  private val buildFileName = "package.json"
  private val npmVersion = """[~<>=v\^]*(.*)""".r

  override def buildRepository(file: File, groupName: String, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Option[Repository] = {
    // project files
    val buildFile = getBuildFile(file)

    val source: String = Source.fromFile(buildFile).getLines.mkString
    val jsonValues = Json.parse(source).as[JsObject].value

    val name = jsonValues.get("name")
      .map(_.as[JsString].value)
      .getOrElse(file.getName)

    val devDependencies = extractDependencies(jsonValues.get("devDependencies"))
    val dependencies = extractDependencies(jsonValues.get("dependencies"))

    Some(Repository(name, groupName, dependencies ++ devDependencies, "NPM", Map.empty[String, String]))
  }

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

