package model

/**
  * A plugin displayed in the view.
  */
abstract class DisplayPlugin(override val name: String,
                             override val latestVersion: String,
                             override val versions: Map[String, Set[String]])
  extends AbstractDisplay(name, latestVersion, versions)

object DisplayPlugin {

  def from(plugin: Plugin,
           latestVersion: String,
           versions: Map[String, Set[String]]): DisplayPlugin = {
    plugin match {
      case _: GradlePlugin => DisplayGradlePlugin(plugin.name, latestVersion, versions)
      case _: MavenPlugin => DisplayMavenPlugin(plugin.name, latestVersion, versions)
    }
  }
}

case class DisplayGradlePlugin(override val name: String,
                               override val latestVersion: String,
                               override val versions: Map[String, Set[String]])
  extends DisplayPlugin(name, latestVersion, versions) {

  override def getIconPath: String = "images/gradle.svg"
}

case class DisplayMavenPlugin(override val name: String,
                              override val latestVersion: String,
                              override val versions: Map[String, Set[String]])
  extends DisplayPlugin(name, latestVersion, versions) {

  override def getIconPath: String = "images/maven.svg"
}
