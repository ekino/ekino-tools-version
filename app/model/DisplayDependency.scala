package model

/**
  * A dependency displayed in the view.
  */
class DisplayDependency(override val name: String,
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
