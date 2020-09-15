package model

import play.api.{ConfigLoader, Configuration}
import play.api.ConfigLoader.stringLoader

case class Config(conf: Configuration) {
  val filePath: String = conf.get("project.repositories.clone-path")
  val npmRegistry = Site(conf.get[Configuration]("npm.registry"))
  val mavenLocal = Site(conf.get[Configuration]("maven.local"))
  val mavenLocalPlugins = Site(conf.get[Configuration]("maven.local-plugins"))
  val mavenCentral = Site(conf.get[Configuration]("maven.central"))
  val mavenGradlePlugins = Site(conf.get[Configuration]("maven.gradle-plugins"))
  val localRepositories: Seq[String] = getLocalRepositories

  private def getLocalRepositories: Seq[String] = {
    conf
      .get[Configuration]("project.repositories")
      .getOptional("local-paths")(ConfigLoader.seqStringLoader)
      .getOrElse(Seq.empty)
  }
}

case class Site(conf: Configuration) {
  val url: String = conf.get("url")
  val user: String = conf.getOptional("user").getOrElse("")
  val password: String = conf.getOptional("password").getOrElse("")
}
