package utils

import model.{Plugin, SpringBootData, Dependency}

import scala.collection.immutable.ListMap

/**
  * Utility object with springboot methods.
  */
object SpringBootUtils {

  def getSpringBootOverrides(artifacts: Seq[Dependency], properties: Map[String, String], springBootData: SpringBootData): Map[String, String] = {
    val result = ListMap.newBuilder[String, String]
    properties
      .filter(p => !artifacts.exists(_.version == p._1))
      .filter(p => springBootData.properties.exists(_._1 == p._1))
      .foreach(p => {
        val maybeTuple = springBootData.artefacts
          .find(a => a._2 == p._1)
          .orElse(None)
        if (maybeTuple.isDefined) {
          result += maybeTuple.get._1 -> p._2
        }
      })
    result.result()
  }

  def getSpringBootData(plugins: Seq[Plugin], springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): SpringBootData = {
    plugins
      .find(_.name.contains("org.springframework.boot"))
      .filter(_.name.startsWith("1."))
      .map(_ => springBootDefaultData)
      .getOrElse(springBootMasterData)
  }

}
