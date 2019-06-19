package model

/**
  * A plugin displayed in the view.
  */
class DisplayPlugin(override val name: String,
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
