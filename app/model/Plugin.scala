package model

/**
  * A single plugin retrieved from an artifact.
  */
sealed abstract class Plugin(val name: String,
                             val version: String)

case class GradlePlugin(override val name: String,
                        override val version: String)
  extends Plugin(name, version)

case class MavenPlugin(override val name: String,
                       override val version: String)
  extends Plugin(name, version)
