package model

import utils.VersionComparator

/**
  * Dependency DTO.
  */
case class DisplayDependency(name: String, centralVersion: String, versions: Map[String, Set[String]]) {

  def getRepositories(dependency: String): Seq[String] = {
    versions
      .get(dependency)
      .map(_.toSeq.sorted)
      .getOrElse(Seq.empty)
  }

  def sortByVersion(s1: String, s2: String): Boolean = {
    VersionComparator.compare(s1, s2) >= 0
  }

  /**
    * Calculates the percentage of repositories up to date.
    *
    * @return the percentage of repositories up to date
    */
  def getCompletionPercentage: Integer = {
    if (versions.isEmpty) {
      return 100
    }
    val size = versions.values.flatten.size
    val count = versions
      .filterKeys(version => VersionComparator.compare(version, centralVersion) >= 0)
      .values.flatten.size

    val result = 100d * count / size
    result.toInt
  }

  /**
    * Calculates the number of uses of this dependency in all the projects.
    *
    * @return the count of the dependency usages
    */
  def numberOfUses(): Int = versions.values.flatten.size
}
