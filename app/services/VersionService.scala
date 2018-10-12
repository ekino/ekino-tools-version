package services

import java.io.File

import javax.inject.{Inject, Singleton}
import model.CustomExecutionContext.executionContextExecutor
import model._
import play.api.{ConfigLoader, Configuration, Logger}
import utils._

import scala.concurrent.{Await, Future}

/**
  * Main versions service.
  */
@Singleton
class VersionService @Inject()(
  configuration: Configuration,
  gitRepositoryService: GitRepositoryService,
  springBootVersionService: SpringBootVersionService) {

  private val config = Config(configuration)
  private val springBootDefaultData = springBootVersionService.computeSpringBootData(false)
  private val springBootMasterData = springBootVersionService.computeSpringBootData(true)

  private var data: RepositoryData = RepositoryData.noData

  /**
    * List all the projects to display.
    *
    * @return a list of Project
    */
  def listProjects(): Seq[Project] = {
    data.repositories
      .map(DisplayRepository(_, data.localDependencies, data.centralDependencies, data.localPlugins, data.gradlePlugins))
      .groupBy(_.project())
      .map(e => Project(e._1, e._2.sortBy(_.name)))
      .toSeq
      .sortWith((a, b) => a.repositories.lengthCompare(b.repositories.size) > 0)
  }

  /**
    * Get a single repository to display.
    *
    * @param name the repository name i.e. gradle rootProject project
    * @return an option of DisplayRepository
    */
  def getRepository(name: String): Option[DisplayRepository] = {
    data.repositories.find(name == _.name)
      .map(DisplayRepository(_, data.localDependencies, data.centralDependencies, data.localPlugins, data.gradlePlugins))
  }

  /**
    * Get a single dependency to display.
    *
    * @param name the dependency name i.e. package:artifactName
    * @return the DisplayDependency or a new one if not found
    */
  def getDependency(name: String): DisplayDependency = {
    val option: Option[DisplayDependency] = data.dependencies.find(_.name == name)
    if (option.isEmpty) {
      val dependency = DisplayDependency(name, data.centralDependencies.getOrElse(name, ""))
      data = data.copy(dependencies = data.dependencies :+ dependency)
      dependency
    } else option.get
  }

  /**
    * Get a single plugin to display.
    *
    * @param pluginId the plugin id ex. com.ekino.base
    * @return the DisplayPlugin or a new one if not found
    */
  def getPlugin(pluginId: String): DisplayPlugin = {
    val option: Option[DisplayPlugin] = data.plugins.find(_.pluginId == pluginId)
    if (option.isEmpty) {
      val plugin = DisplayPlugin(pluginId, data.localPlugins.getOrElse(pluginId, ""))
      data = data.copy(plugins = data.plugins :+ plugin)
      plugin
    } else option.get
  }

  def allDependencies(): Seq[DisplayDependency] = data.dependencies.sortBy(_.name)

  /**
    * Initialize the data if needed.
    */
  def initData(): Unit = {
    if (data.repositories.isEmpty) {
      val start = System.currentTimeMillis
      fetchRepositories()
      Logger.info(s"data has been initialized in ${System.currentTimeMillis - start} ms")
    }
  }

  /**
    * Fetch repositories data as repository list, versions, dependencies ...
    */
  def fetchRepositories() {
    Logger.info("Processing... this will take few seconds")

    val workspace: File = new File(config.filePath)
    if (!workspace.exists) {
      gitRepositoryService.updateGitRepositories()
    }

    clearCaches()

    computeRepositories(workspace)

    computePluginVersions()

    computeDependencyVersions()

    computePlugins()

    computeDependencies()

  }

  /**
    * Clear all the internal cache objects
    */
  private def clearCaches(): Unit = {
    data = RepositoryData.noData
  }

  /**
    * Parse all repositories from workspace.
    *
    * @param workspace the parent directory of the repositories
    */
  private def computeRepositories(workspace: File): Unit = {
    data = data.copy(repositories = workspace.listFiles()
      .flatMap(computeRepositoriesForGroup))
  }

  /**
    * Parse all repositories from the group folder.
    *
    * @param groupFolder the parent directory of the repositories for this group
    * @return a list of Repository
    */
  private def computeRepositoriesForGroup(groupFolder: File): Seq[Repository] = {
    groupFolder.listFiles
      .filter(_.isDirectory)
      .flatMap(parseDirectory(_, groupFolder.getName))
      .filter(repo => repo.versions.nonEmpty || repo.plugins.nonEmpty)

  }

  private def parseDirectory(projectFolder: File, groupName: String): Option[Repository] = {
    projectFolder match {
      case npm if NPMRepositoryParser.canProcess(npm) =>
        NPMRepositoryParser.buildRepository(npm, groupName)
      case gradle if GradleRepositoryParser.canProcess(gradle) =>
        GradleRepositoryParser.buildRepository(gradle, groupName, springBootDefaultData, springBootMasterData)
      case mvn if MavenRepositoryParser.canProcess(mvn) =>
        MavenRepositoryParser.buildRepository(mvn, groupName, springBootDefaultData, springBootMasterData)
      case sbt if SBTRepositoryParser.canProcess(sbt) =>
        SBTRepositoryParser.buildRepository(sbt, groupName)
      case _ =>
        None
    }
  }

  /**
    * Compute local plugin versions.
    */
  private def computePluginVersions(): Unit = {
    import config._

    var localPluginFutures = Map.empty[String, Future[(String, String)]]
    var gradlePluginFutures = Map.empty[String, Future[(String, String)]]

    data.repositories.foreach(r => {
      r.plugins.foreach(v => {
        if (isHigherVersion(data.localPlugins.get(v._1), v._2)) {
          data = data.copy(localPlugins = data.localPlugins + (v._1 -> v._2))
        }
        if (!localPluginFutures.contains(v._1)) {
          localPluginFutures += v._1 -> MavenVersionFetcher.getLatestVersion(getPluginCoordinates(v._1), mavenLocalPlugins.url, mavenLocalPlugins.user, mavenLocalPlugins.password)
        }
        if (!gradlePluginFutures.contains(v._1)) {
          gradlePluginFutures += v._1 -> MavenVersionFetcher.getLatestVersion(getPluginCoordinates(v._1), mavenGradlePlugins.url, mavenGradlePlugins.user, mavenGradlePlugins.password)
        }
      })
    })
    val sequence = Future.sequence(gradlePluginFutures.values ++ localPluginFutures.values)

    // waiting for all the futures
    val timeout = configuration.get("timeout.compute-plugins")(ConfigLoader.finiteDurationLoader)
    val list = Await.result(sequence, timeout)

    list.foreach { element =>
      val pluginId = element._1.split(':')(0)
      if (isHigherVersion(data.gradlePlugins.get(pluginId), element._2)) {
        data = data.copy(gradlePlugins = data.gradlePlugins + (pluginId -> element._2))
      }
    }
  }

  private def getPluginCoordinates(pluginId: String): String = pluginId + ":" + pluginId + ".gradle.plugin"

  /**
    * Compute local and central versions.
    */
  private def computeDependencyVersions(): Unit = {
    import config._

    var localDependencyFutures = Map.empty[String, Future[(String, String)]]
    var centralDependencyFutures = Map.empty[String, Future[(String, String)]]
    var npmDependencyFutures = Map.empty[String, Future[(String, String)]]

    data.repositories.foreach(r => {
      r.versions.foreach(v => {
        if (isHigherVersion(data.localDependencies.get(v._1), v._2)) {
          data = data.copy(localDependencies = data.localDependencies + (v._1 -> v._2))
        }
        if (MavenVersionFetcher.isMavenVersion(v._1)) {
          if (!localDependencyFutures.contains(v._1)) {
            localDependencyFutures += v._1 -> MavenVersionFetcher.getLatestVersion(v._1, mavenLocal.url, mavenLocal.user, mavenLocal.password)
          }
          if (!centralDependencyFutures.contains(v._1)) {
            centralDependencyFutures += v._1 -> MavenVersionFetcher.getLatestVersion(v._1, mavenCentral.url, mavenCentral.user, mavenCentral.password)
          }
        } else if (!npmDependencyFutures.contains(v._1)) {
          npmDependencyFutures += v._1 -> NpmVersionFetcher.getLatestVersion(v._1, npmRegistryUrl)
        }
      })
    })
    val sequence = Future.sequence(centralDependencyFutures.values ++ localDependencyFutures.values ++ npmDependencyFutures.values)

    // waiting for all the futures
    val timeout = configuration.get("timeout.compute-versions")(ConfigLoader.finiteDurationLoader)
    val list = Await.result(sequence, timeout)

    list.foreach { element =>
      if (isHigherVersion(data.centralDependencies.get(element._1), element._2)) {
        data = data.copy(centralDependencies = data.centralDependencies + (element._1 -> element._2))
      }
    }
  }

  /**
    * Compute plugin map from repositories
    */
  private def computePlugins(): Unit = {
    data.repositories.foreach(r => {
      r.plugins.foreach(t => {
        val plugin = getPlugin(t._1)
        plugin.versions += t._2 -> (plugin.versions.getOrElse(t._2, Set.empty[String]) + r.name)
      })
    })
  }

  /**
    * Compute dependency map from repositories
    */
  private def computeDependencies(): Unit = {
    data.repositories.foreach(r => {
      r.versions.foreach(t => {
        val dependency = getDependency(t._1)
        dependency.versions += t._2 -> (dependency.versions.getOrElse(t._2, Set.empty[String]) + r.name)
      })
    })
  }

  private def isHigherVersion(existingVersion: Option[String], newVersion: String): Boolean = {
    existingVersion.isEmpty || VersionComparator.versionCompare(existingVersion.get, newVersion) < 0
  }

}
