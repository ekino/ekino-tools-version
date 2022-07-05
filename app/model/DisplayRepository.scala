package model

import utils.VersionComparator

import scala.util.matching.Regex

/**
  * Repository DTO.
  */
case class DisplayRepository(
  repository: Repository,
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
    * @param dependency the dependency
    * @return True when the dependency is up to date
    */
  def isVersionUpToDate(dependency: Dependency): Boolean = {
    val version = dependency.version
    val centralReference = getCentralDependencyVersion(dependency.name)
    val reference = if (centralReference.isEmpty) getLocalDependencyVersion(dependency.name) else centralReference
    VersionComparator.compare(version, reference) >= 0
  }

  def springBootVersion(dependency: Dependency, springBootData: SpringBootData): String = {
    val springbootVersion = repository.plugins.find(_.name == "org.springframework.boot")

    if (springBootData.artefacts.contains(dependency.name) && springbootVersion.isDefined) {

      val placeholder = springBootData.artefacts(dependency.name)

      val version = if (springBootData.properties.contains(placeholder)) {
        springBootData.properties(placeholder)
      } else if (placeholder == "revision") {
        springbootVersion.get.version
      } else {
        placeholder
      }

      VersionComparator.compare(dependency.version, version).sign match {
        case -1 => "springboot-down"
        case  0 => "springboot-equal"
        case  1 => "springboot-up"
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
    val gradleReference = getGradlePluginVersion(pluginId)
    val reference = if (gradleReference.isEmpty) getLocalPluginVersion(pluginId) else gradleReference
    VersionComparator.compare(version, reference) >= 0
  }

  def getDependencyVersion(dependency: String, subfolder: String): String = repository.dependencies.find(dep => dep.name.equals(dependency) && dep.subfolder.equals(subfolder)).map(_.version).getOrElse("")
  def getLocalDependencyVersion(dependency: String): String = localDependencies.getOrElse(dependency, "")
  def getCentralDependencyVersion(dependency: String): String = centralDependencies.getOrElse(dependency, "")
  def getPluginVersion(pluginId: String): String = repository.plugins.find(_.name.equals(pluginId)).map(_.version).getOrElse("")
  def getLocalPluginVersion(pluginId: String): String = localPlugins.getOrElse(pluginId, "")
  def getGradlePluginVersion(pluginId: String): String = gradlePlugins.getOrElse(pluginId, "")
  def project: String = repository.group

  /**
    * Checks that the given dependency is the first having this subfolder.
    */
  def isFirstOfSubfolder(dependency: Dependency): Boolean = {
    repository.dependencies
      .filter(_.subfolder.nonEmpty)
      .find(_.subfolder.equals(dependency.subfolder))
      .exists(_.equals(dependency))
  }

  implicit class RegexOps(sc: StringContext) {
    def r = new Regex(sc.parts.mkString)
  }

  private[this] def getRepositoryType(name: String) = name match {
    case r".*library.*" => "library"
    case r".*tool.*" => "tool"
    case r".*service.*" => "service"
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
    val size = repository.dependencies.size + repository.plugins.size
    if (size == 0) {
      return 100
    }
    val countVersion = repository
      .dependencies
      .count(isVersionUpToDate)
      .asInstanceOf[Double]

    val countPlugin = repository
      .plugins
      .count(p => isPluginUpToDate(p.name))
      .asInstanceOf[Double]

    val result = 100 * ((countVersion + countPlugin) / size)
    result.toInt
  }

}
