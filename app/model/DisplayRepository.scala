package model

import utils.VersionComparator


/**
  * Repository DTO.
  */
case class DisplayRepository(repository: Repository, mergedValues: Map[String, String], mvnValues: Map[String, String],
                             pluginValues: Map[String, String]) {

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
    val version = getVersion(dependency)
    var reference = getMvnVersion(dependency)
    if (reference.isEmpty) reference = getProjectVersion(dependency)
    VersionComparator.versionCompare(version, reference) >= 0
  }

  def springBootVersion(dependency: String): String = {
    if (repository.springBootData != null && repository.springBootData.artefacts.contains(dependency)
      && repository.plugins.contains("org.springframework.boot")) {
      val compare = VersionComparator.versionCompare(getVersion(dependency), repository.springBootData.properties(repository.springBootData.artefacts(dependency)))
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
    val pluginVersion = getPluginVersion(pluginId)
    val projectPluginVersion = getProjectPluginVersion(pluginId)
    VersionComparator.versionCompare(pluginVersion, projectPluginVersion) >= 0
  }

  def getProjectVersion(dependency: String): String = mergedValues.getOrElse(dependency, null)
  def getMvnVersion(dependency: String): String = mvnValues.getOrElse(dependency, null)
  def getVersion(dependency: String): String = repository.versions.getOrElse(dependency, null)
  def getPluginVersion(pluginId: String): String = repository.plugins.getOrElse(pluginId, null)
  def getProjectPluginVersion(pluginId: String): String = pluginValues.getOrElse(pluginId, null)
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





