package utils

import model.SpringBootData

import scala.collection.immutable.ListMap

/**
  * Utility object with springboot methods.
  */
object SpringBootUtils {

  def getSpringBootOverrides(artifacts: Map[String, String], properties: Map[String, String], springBootData: SpringBootData): Map[String, String] = {
    var result = ListMap.empty[String, String]
    properties
      .filter(p => !artifacts.exists(_._2 == p._1))
      .filter(p => springBootData.properties.exists(_._1 == p._1))
      .foreach(p => {
        val maybeTuple = springBootData.artefacts
          .find(a => a._2 == p._1)
          .orElse(None)
        if (maybeTuple.isDefined) {
          result += maybeTuple.get._1 -> p._2
        }
      })
    result
  }

  def getSpringBootData(plugins: Map[String, String], springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): SpringBootData = {
    plugins.get("org.springframework.boot")
      .filter(_.startsWith("1."))
      .map(_ => springBootDefaultData)
      .getOrElse(springBootMasterData)
  }

}
