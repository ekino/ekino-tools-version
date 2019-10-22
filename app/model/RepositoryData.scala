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
  gradlePlugins: Map[String, String],
  springBootDefaultData: SpringBootData,
  springBootMasterData: SpringBootData
) {

  def getPluginsAndDependencies: Seq[AbstractDisplay] = {
    dependencies ++ plugins
  }
}

object RepositoryData {
  def noData = RepositoryData(
    Seq.empty[Repository],
    Seq.empty[DisplayDependency],
    Seq.empty[DisplayPlugin],
    Map.empty[String, String],
    Map.empty[String, String],
    Map.empty[String, String],
    Map.empty[String, String],
    SpringBootData.noData,
    SpringBootData.noData
  )
}
