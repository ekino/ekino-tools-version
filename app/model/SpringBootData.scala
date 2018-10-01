package model

/**
  * Class holding springboot data.
  */
case class SpringBootData(artefacts: Map[String, String], properties: Map[String, String])

object SpringBootData {
  val noData = SpringBootData(Map.empty, Map.empty)
}
