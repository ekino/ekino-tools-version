package model

import utils.{ImageHelper, VersionComparator}

sealed abstract class AbstractDisplay(
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
      .view.filterKeys(VersionComparator.compare(_, latestVersion) >= 0)
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

/**
  * A plugin displayed in the view.
  */
case class DisplayPlugin(override val name: String,
  override val latestVersion: String,
  override val versions: Map[String, Set[String]],
  override val dependencyType: String
) extends AbstractDisplay(name, latestVersion, versions, dependencyType)

object DisplayPlugin {

  def from(
    plugin: Plugin,
    latestVersion: String,
    versions: Map[String, Set[String]]): DisplayPlugin = new DisplayPlugin(plugin.name, latestVersion, versions, plugin.getType)
}

/**
  * A dependency displayed in the view.
  */
case class DisplayDependency(override val name: String,
  override val latestVersion: String,
  override val versions: Map[String, Set[String]],
  override val dependencyType: String)
  extends AbstractDisplay(name, latestVersion, versions, dependencyType)

object DisplayDependency {

  def from(
    version: Dependency,
    latestVersion: String,
    versions: Map[String, Set[String]]): DisplayDependency = new DisplayDependency(version.name, latestVersion, versions, version.getType)
}

