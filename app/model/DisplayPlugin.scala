package model

/**
  * Dependency DTO.
  */
case class DisplayPlugin(pluginId: String, gradleVersion: String, versions: Map[String, Set[String]])
  extends AbstractDisplay(pluginId, gradleVersion, versions)
