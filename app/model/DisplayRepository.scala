package model

import utils.VersionComparator


/**
  * Repository DTO.
  */
case class DisplayRepository(repository: Repository,
                             localDependencies: Map[String, String],
                             centralDependencies: Map[String, String],
                             localPlugins: Map[String, String],
                             gradlePlugins: Map[String, String]) {

  val name: String = repository.name
  val repositoryType: String = getRepositoryType(repository.name)
  val completionPercentage: Integer = calculateCompletionPercentage()

  /**
    * Indicates whether a dependency version is up-to-date with other repositories.
    *
    * @param dependency the dependency id
    * @return True when the dependency is up to date
    */
  def isVersionUpToDate(dependency: String): Boolean = {
    val version = getDependencyVersion(dependency)
    var reference = getCentralDependencyVersion(dependency)
    if (reference.isEmpty) reference = getLocalDependencyVersion(dependency)
    VersionComparator.versionCompare(version, reference) >= 0
  }

  def springBootVersion(dependency: String): String = {
    if (repository.springBootData != null && repository.springBootData.artefacts.contains(dependency)
      && repository.plugins.contains("org.springframework.boot")) {
      val compare = VersionComparator.versionCompare(getDependencyVersion(dependency), repository.springBootData.properties(repository.springBootData.artefacts(dependency)))
      if (compare == 0) {
        "springboot-equal"
      } else if (compare > 0) {
        "springboot-up"
      } else {
        "springboot-down"
      }
    } else {
      ""
    }
  }

  /**
    * Indicates whether a plugin version is up-to-date with other repositories.
    *
    * @param pluginId the plugin id
    * @return True when the plugin is up to date
    */
  def isPluginUpToDate(pluginId: String): Boolean = {
    val version = getPluginVersion(pluginId)
    var reference = getGradlePluginVersion(pluginId)
    if (reference.isEmpty) reference = getLocalPluginVersion(pluginId)
    VersionComparator.versionCompare(version, reference) >= 0
  }

  def getDependencyVersion(dependency: String): String = repository.versions.getOrElse(dependency, "")
  def getLocalDependencyVersion(dependency: String): String = localDependencies.getOrElse(dependency, "")
  def getCentralDependencyVersion(dependency: String): String = centralDependencies.getOrElse(dependency, "")
  def getPluginVersion(pluginId: String): String = repository.plugins.getOrElse(pluginId, "")
  def getLocalPluginVersion(pluginId: String): String = localPlugins.getOrElse(pluginId, "")
  def getGradlePluginVersion(pluginId: String): String = gradlePlugins.getOrElse(pluginId, "")
  def project(): String = repository.group

  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  private[this] def getRepositoryType(name: String) = name match {
    case r".*library.*" => "library"
    case r".*tool.*" => "tool"
    case r".*service.*" => "service"
    case r".*spring.*" => "spring"
    case r".*lambda.*" => "cloud"
    case r".*-gradle-.*" => "plug"
    case _ => "default"
  }

  /**
    * Calculates the percentage of dependencies up to date.
    *
    * @return the percentage of dependencies up to date
    */
  private[this] def calculateCompletionPercentage(): Integer = {
    val size = repository.versions.size + repository.plugins.size
    if (size == 0) {
      return 100
    }
    val countVersion = repository
      .versions
      .count(p => isVersionUpToDate(p._1))
      .asInstanceOf[Double]

    val countPlugin = repository
      .plugins
      .count(p => isPluginUpToDate(p._1))
      .asInstanceOf[Double]

    val result = 100 * ((countVersion + countPlugin) / size)
    result.toInt
  }

}





