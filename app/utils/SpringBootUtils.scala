package utils

import model.{Dependency, Plugin, SpringBootData}
import services.SpringBootVersionService

/**
  * Utility object with springboot methods.
  */
object SpringBootUtils {

  def getSpringBootOverrides(artifacts: Seq[Dependency], properties: Map[String, String], springBootData: SpringBootData): Map[String, String] =
    properties
      .filter(p => !artifacts.exists(_.version == p._1))
      .filter(p => springBootData.properties.exists(_._1 == p._1))
      .map(p => springBootData.artefacts
        .find(a => a._2 == p._1)
        .map(a => (a._1, p._2))
      )
      .flatten
      .toMap

  def getSpringBootData(plugins: Seq[Plugin]): SpringBootData = {
    plugins
      .find(_.name.contains("org.springframework.boot"))
      .map(plugin => SpringBootVersionService.springBootData(plugin.version))
      .getOrElse(SpringBootData.noData)
  }

}
