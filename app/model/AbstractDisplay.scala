package model

import utils.{ImageHelper, VersionComparator}

abstract class AbstractDisplay(
  val name: String,
  val latestVersion: String,
  val versions: Map[String, Set[String]],
  val dependencyType: String
) {

  def getRepositories(plugin: String): Seq[String] =
    versions
      .get(plugin)
      .map(_.toSeq.sorted)
      .getOrElse(Seq.empty)

  def sortByVersion(s1: String, s2: String): Boolean =
    VersionComparator.compare(s1, s2) >= 0

  /**
    * Calculates the number of uses of this dependency in all the projects.
    *
    * @return the count of the dependency usages
    */
  def numberOfUses(): Int = versions.values.flatten.size

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
      .filterKeys(VersionComparator.compare(_, latestVersion) >= 0)
      .values.flatten.size

    val result = 100d * count / size
    result.toInt
  }

  /**
    * Specifies the path to the displayed icon in the assets.
    * For instance for ''my-image.jpg'' found in ''public/images'',
    * the method should return ''images/my-image.jpg''
    */
  def getIconPath: String = ImageHelper.getIconPath(dependencyType)
}
