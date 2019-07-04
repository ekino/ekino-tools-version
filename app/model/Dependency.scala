package model

/**
  * A single dependency retrieved from an artifact.
  */
sealed abstract class Dependency(val name: String,
                                 val version: String,
                                 val subfolder: String) {
  def getType: String
}

case class JvmDependency(override val name: String,
                         override val version: String,
                         override val subfolder: String)
  extends Dependency(name, version, subfolder) {
  override def getType: String = "java"
}

case class NodeDependency(override val name: String,
                          override val version: String,
                          override val subfolder: String)
  extends Dependency(name, version, subfolder) {
  override def getType: String = "nodejs"
}
