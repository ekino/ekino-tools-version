package model

import play.api.ConfigLoader.stringLoader
import play.api.Configuration

case class Config(conf: Configuration) {
  val filePath: String = conf.get("project.repositories.path")
  val npmRegistryUrl: String = conf.get("npm.registry.url")
  val mavenLocal = Site(conf.get[Configuration]("maven.local"))
  val mavenLocalPlugins = Site(conf.get[Configuration]("maven.local-plugins"))
  val mavenCentral = Site(conf.get[Configuration]("maven.central"))
  val mavenGradlePlugins = Site(conf.get[Configuration]("maven.gradle-plugins"))
}

case class Site(conf: Configuration) {
  val url: String = conf.get("url")
  val user: String = conf.getOptional("user").getOrElse("")
  val password: String = conf.getOptional("password").getOrElse("")
}
