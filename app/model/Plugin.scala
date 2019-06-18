package model

/**
  * A single plugin retrieved from an artifact.
  */
sealed abstract class Plugin(val name: String,
                             val version: String) {
  def getType: String
}

case class GradlePlugin(override val name: String,
                        override val version: String)
  extends Plugin(name, version) {
  override def getType: String = "gradle"
}

case class MavenPlugin(override val name: String,
                       override val version: String)
  extends Plugin(name, version) {
  override def getType: String = "maven"
}
