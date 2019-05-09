package model

/**
  * A dependency displayed in the view.
  */
abstract class DisplayDependency(override val name: String,
                                 override val latestVersion: String,
                                 override val versions: Map[String, Set[String]])
  extends AbstractDisplay(name, latestVersion, versions)

object DisplayDependency {

  def from(version: Dependency,
           latestVersion: String,
           versions: Map[String, Set[String]]): DisplayDependency = {
    version match {
      case _: JvmDependency => DisplayJavaDependency(version.name, latestVersion, versions)
      case _: NodeDependency => DisplayNpmDependency(version.name, latestVersion, versions)
    }
  }
}

case class DisplayJavaDependency(override val name: String,
                                 override val latestVersion: String,
                                 override val versions: Map[String, Set[String]])
  extends DisplayDependency(name, latestVersion, versions) {

  override def getIconPath: String = "images/java.svg"
}

case class DisplayNpmDependency(override val name: String,
                                override val latestVersion: String,
                                override val versions: Map[String, Set[String]])
  extends DisplayDependency(name, latestVersion, versions) {

  override def getIconPath: String = "images/nodejs.svg"
}
