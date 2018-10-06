package services

import java.io.File

import javax.inject.{Inject, Singleton}
import model.CustomExecutionContext.executionContextExecutor
import model._
import play.api.ConfigLoader.stringLoader
import play.api.{ConfigLoader, Configuration, Logger}
import utils.{GradleRepositoryParser, MavenRepositoryParser, SBTRepositoryParser, VersionComparator}

import scala.concurrent.{Await, Future}

/**
  * Main versions service.
  */
@Singleton
class VersionService @Inject()(configuration: Configuration, fetcher: MavenVersionFetcher,
                               gitRepositoryService: GitRepositoryService, springBootVersionService: SpringBootVersionService) {

  private val filePath = configuration.get("project.repositories.path")
  private val localMavenUrl = configuration.get("maven.local.url")
  private val localMavenUser = configuration.getOptional("maven.local.user").getOrElse("")
  private val localMavenPassword = configuration.getOptional("maven.local.password").getOrElse("")
  private val localPluginsMavenUrl = configuration.get("maven.local-plugins.url")
  private val localPluginsMavenUser = configuration.getOptional("maven.local-plugins.user").getOrElse("")
  private val localPluginsMavenPassword = configuration.getOptional("maven.local-plugins.password").getOrElse("")
  private val centralMavenUrl = configuration.get("maven.central.url")
  private val centralMavenUser = configuration.getOptional("maven.central.user").getOrElse("")
  private val centralMavenPassword = configuration.getOptional("maven.central.password").getOrElse("")
  private val gradlePluginsMavenUrl = configuration.get("maven.gradle-plugins.url")
  private val gradlePluginsMavenUser = configuration.getOptional("maven.gradle-plugins.user").getOrElse("")
  private val gradlePluginsMavenPassword = configuration.getOptional("maven.gradle-plugins.password").getOrElse("")

  private var repositories = Seq.empty[Repository]
  private var dependencies = Seq.empty[DisplayDependency]
  private var plugins = Seq.empty[DisplayPlugin]
  private var localDependencies = Map.empty[String, String]
  private var centralDependencies = Map.empty[String, String]
  private var localPlugins = Map.empty[String, String]
  private var gradlePlugins = Map.empty[String, String]

  private val springBootDefaultData = springBootVersionService.computeSpringBootData(false)
  private val springBootMasterData = springBootVersionService.computeSpringBootData(true)

  /**
    * Fetch repositories data as repository list, versions, dependencies ...
    */
  def fetchRepositories() {
    val start = System.currentTimeMillis

    Logger.info("Processing... this will take few seconds")

    val workspace: File = new File(filePath)
    if (!workspace.exists) {
      gitRepositoryService.updateGitRepositories()
    }

    clearCaches()

    computeRepositories(workspace)

    computePluginVersions()

    computeDependencyVersions()

    computePlugins()

    computeDependencies()

    Logger.info("took " + (System.currentTimeMillis - start) + " ms to get data")
  }

  /**
    * List all the projects to display.
    *
    * @return a list of Project
    */
  def listProjects(): Seq[Project] = {
    if (repositories.isEmpty) {
      fetchRepositories()
    }
    repositories
      .map(DisplayRepository(_, localDependencies, centralDependencies, localPlugins, gradlePlugins))
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
    if (repositories.isEmpty) {
      fetchRepositories()
    }
    repositories.find(name == _.name)
      .map(DisplayRepository(_, localDependencies, centralDependencies, localPlugins, gradlePlugins))
  }

  /**
    * Get a single dependency to display.
    *
    * @param name the dependency name i.e. package:artifactName
    * @return the DisplayDependency or a new one if not found
    */
  def getDependency(name: String): DisplayDependency = {
    if (repositories.isEmpty) {
      fetchRepositories()
    }
    val option: Option[DisplayDependency] = dependencies.find(_.name == name)
    if (option.isEmpty) {
      val dependency = DisplayDependency(name, centralDependencies.getOrElse(name, ""))
      dependencies :+= dependency
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
    if (repositories.isEmpty) {
      fetchRepositories()
    }
    val option: Option[DisplayPlugin] = plugins.find(_.pluginId == pluginId)
    if (option.isEmpty) {
      val plugin = DisplayPlugin(pluginId, localPlugins.getOrElse(pluginId, ""))
      plugins :+= plugin
      plugin
    } else option.get
  }

  def allDependencies(): Seq[DisplayDependency] = dependencies.sortBy(_.name)

  /**
    * Clear all the internal cache objects
    */
  private def clearCaches(): Unit = {
    repositories = Seq.empty[Repository]
    dependencies = Seq.empty[DisplayDependency]
    plugins = Seq.empty[DisplayPlugin]
    localDependencies = Map.empty[String, String]
    centralDependencies = Map.empty[String, String]
    localPlugins = Map.empty[String, String]
    gradlePlugins = Map.empty[String, String]
  }

  /**
    * Parse all repositories from workspace.
    *
    * @param workspace the parent directory of the repositories
    */
  private def computeRepositories(workspace: File): Unit = {
    repositories = workspace.listFiles()
      .flatMap(computeRepositoriesForGroup)
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
    if (GradleRepositoryParser.getBuildFile(projectFolder).exists()) {
      Logger.debug(s"gradle project: $projectFolder")
      GradleRepositoryParser.buildRepository(projectFolder, groupName, springBootDefaultData, springBootMasterData)
    } else if (MavenRepositoryParser.getBuildFile(projectFolder).exists()) {
      Logger.debug(s"maven project: $projectFolder")
      MavenRepositoryParser.buildRepository(projectFolder, groupName, springBootDefaultData, springBootMasterData)
    } else if (SBTRepositoryParser.getBuildFile(projectFolder).exists()) {
      Logger.debug(s"sbt project: $projectFolder")
      SBTRepositoryParser.buildRepository(projectFolder, groupName)
    } else None
  }

  /**
    * Compute local plugin versions.
    */
  private def computePluginVersions(): Unit = {
    var localPluginFutures = Map.empty[String, Future[(String, String)]]
    var gradlePluginFutures = Map.empty[String, Future[(String, String)]]

    repositories.foreach(r => {
      r.plugins.foreach(v => {
        if (isHigherVersion(localPlugins.get(v._1), v._2)) {
          localPlugins += v._1 -> v._2
        }
        if (!localPluginFutures.contains(v._1)) {
          localPluginFutures += v._1 -> fetcher.getLatestVersion(getPluginCoordinates(v._1), localPluginsMavenUrl, localPluginsMavenUser, localPluginsMavenPassword)
        }
        if (!gradlePluginFutures.contains(v._1)) {
          gradlePluginFutures += v._1 -> fetcher.getLatestVersion(getPluginCoordinates(v._1), gradlePluginsMavenUrl, gradlePluginsMavenUser, gradlePluginsMavenPassword)
        }
      })
    })
    val sequence = Future.sequence(gradlePluginFutures.values ++ localPluginFutures.values)

    // waiting for all the futures
    val timeout = configuration.get("timeout.compute-plugins")(ConfigLoader.finiteDurationLoader)
    val list = Await.result(sequence, timeout)

    list.foreach { element =>
      val pluginId = element._1.split(':')(0)
      if (isHigherVersion(gradlePlugins.get(pluginId), element._2)) {
        gradlePlugins += pluginId -> element._2
      }
    }
  }

  private def getPluginCoordinates(pluginId: String): String = pluginId + ":" + pluginId + ".gradle.plugin"

  /**
    * Compute local and central versions.
    */
  private def computeDependencyVersions(): Unit = {
    var localDependencyFutures = Map.empty[String, Future[(String, String)]]
    var centralDependencyFutures = Map.empty[String, Future[(String, String)]]

    repositories.foreach(r => {
      r.versions.foreach(v => {
        if (isHigherVersion(localDependencies.get(v._1), v._2)) {
          localDependencies += v._1 -> v._2
        }
        if (!localDependencyFutures.contains(v._1)) {
          localDependencyFutures += v._1 -> fetcher.getLatestVersion(v._1, localMavenUrl, localMavenUser, localMavenPassword)
        }
        if (!centralDependencyFutures.contains(v._1)) {
          centralDependencyFutures += v._1 -> fetcher.getLatestVersion(v._1, centralMavenUrl, centralMavenUser, centralMavenPassword)
        }
      })
    })
    val sequence = Future.sequence(centralDependencyFutures.values ++ localDependencyFutures.values)

    // waiting for all the futures
    val timeout = configuration.get("timeout.compute-versions")(ConfigLoader.finiteDurationLoader)
    val list = Await.result(sequence, timeout)

    list.foreach { element =>
      if (isHigherVersion(centralDependencies.get(element._1), element._2)) {
        centralDependencies += element._1 -> element._2
      }
    }
  }

  /**
    * Compute plugin map from repositories
    */
  private def computePlugins(): Unit = {
    repositories.foreach(r => {
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
    repositories.foreach(r => {
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
