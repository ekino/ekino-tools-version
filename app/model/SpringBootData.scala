package model

/**
  * Class holding springboot data.
  */
case class SpringBootData(artifacts: Map[String, String], properties: Map[String, String])

object SpringBootData {
  val noData = SpringBootData(Map.empty, Map.empty)
}
