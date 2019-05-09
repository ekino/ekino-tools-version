package model

/**
  * A single dependency retrieved from an artifact.
  */
sealed abstract class Dependency(val name: String,
                                 val version: String)

case class JvmDependency(override val name: String,
                         override val version: String)
  extends Dependency(name, version)

case class NodeDependency(override val name: String,
                          override val version: String)
  extends Dependency(name, version)
