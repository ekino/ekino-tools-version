package utils

object DependencyHelper {

  /**
    * Checks that a depedency contains a package delimiter. Can be useful to identify java dependencies.
    *
    * @param dependency the dependency name
    * @return true if the dependency contains a colon.
    */
  def isWithPackage(dependency: String): Boolean = dependency != null && dependency.contains(":")
}
