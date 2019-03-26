package model

/**
  * Dependency DTO.
  */
case class DisplayDependency(override val name: String,
                             override val version: String,
                             override val versions: Map[String, Set[String]])
  extends AbstractDisplay(name, version, versions)
