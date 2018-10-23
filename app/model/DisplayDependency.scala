package model

/**
  * Dependency DTO.
  */
case class DisplayDependency(name: String, centralVersion: String, versions: Map[String, Set[String]])
  extends AbstractDisplay(name, centralVersion, versions) {

  /**
    * Calculates the number of uses of this dependency in all the projects.
    *
    * @return the count of the dependency usages
    */
  def numberOfUses(): Int = versions.values.flatten.size
}
