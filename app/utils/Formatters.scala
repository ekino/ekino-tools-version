package utils

import model._
import play.api.libs.json.{Json, OFormat}

object Formatters {
  implicit val dependencyFormat: OFormat[Dependency] = Json.format[Dependency]
  implicit val jvmDependencyFormat: OFormat[JvmDependency] = Json.format[JvmDependency]
  implicit val nodeDependency: OFormat[NodeDependency] = Json.format[NodeDependency]
  implicit val pluginFormat: OFormat[Plugin] = Json.format[Plugin]
  implicit val gradlePluginFormat: OFormat[GradlePlugin] = Json.format[GradlePlugin]
  implicit val mavenPluginFormat: OFormat[MavenPlugin] = Json.format[MavenPlugin]
  implicit val repositoryFormat: OFormat[Repository] = Json.format[Repository]
  implicit val abstractDisplayFormat: OFormat[AbstractDisplay] = Json.format[AbstractDisplay]
  implicit val displayPluginFormat: OFormat[DisplayPlugin] = Json.format[DisplayPlugin]
  implicit val displayDependencyFormat: OFormat[DisplayDependency] = Json.format[DisplayDependency]
  implicit val displayRepositoryFormat: OFormat[DisplayRepository] = Json.format[DisplayRepository]
  implicit val projectFormat: OFormat[Project] = Json.format[Project]
}
