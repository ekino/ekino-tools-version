package model

/**
  * Computed repository data.
  */
case class RepositoryData(
  repositories: Seq[Repository],
  dependencies: Seq[DisplayDependency],
  plugins: Seq[DisplayPlugin],
  localDependencies: Map[String, String],
  centralDependencies: Map[String, String],
  localPlugins: Map[String, String],
  gradlePlugins: Map[String, String]
) {

  def getPluginsAndDependencies: Seq[AbstractDisplay] = {
    dependencies ++ plugins
  }
}

object RepositoryData {
  val emptyData: RepositoryData = RepositoryData(
    Seq.empty[Repository],
    Seq.empty[DisplayDependency],
    Seq.empty[DisplayPlugin],
    Map.empty[String, String],
    Map.empty[String, String],
    Map.empty[String, String],
    Map.empty[String, String]
  )
}
