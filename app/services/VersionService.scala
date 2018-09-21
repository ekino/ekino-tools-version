package services

import java.io.File

import javax.inject.{Inject, Singleton}
import model._
import play.api.ConfigLoader.stringLoader
import play.api.{Configuration, Logger}
import utils.{GradleRepositoryParser, MavenRepositoryParser, SBTRepositoryParser, VersionComparator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Main versions service.
  */
@Singleton
class VersionService @Inject()(configuration: Configuration, fetcher: MavenVersionFetcher,
                               gitRepositoryService: GitRepositoryService, springBootVersionService: SpringBootVersionService) {

  private val filePath = configuration.get("project.repositories.path")
  private val localMavenUrl = configuration.get("local.maven.url")
  private val localMavenUser = configuration.getOptional("local.maven.user").getOrElse("")
  private val localMavenPassword = configuration.getOptional("local.maven.password").getOrElse("")
  private val centralMavenUrl = configuration.get("central.maven.url")
  private val centralMavenUser = configuration.getOptional("central.maven.user").getOrElse("")
  private val centralMavenPassword = configuration.getOptional("central.maven.password").getOrElse("")

  private var repositories = Seq.empty[Repository]
  private var dependencies = Seq.empty[DisplayDependency]
  private var plugins = Seq.empty[DisplayPlugin]
  private var mergedValues = Map.empty[String, String]
  private var mvnValues = Map.empty[String, String]
  private var pluginValues = Map.empty[String, String]
  private var springBootDefaultData = SpringBootData(Map.empty[String, String], Map.empty[String, String])
  private var springBootMasterData = SpringBootData(Map.empty[String, String], Map.empty[String, String])

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

    computeSpringBootData()

    computeRepositories(workspace)

    computeProjectPlugins()

    computeVersions()

    computeDependencies()

    computePlugins()

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
      .map(DisplayRepository(_, mergedValues, mvnValues, pluginValues))
      .groupBy(_.project())
      .map(e => Project(e._1, e._2.sortBy(_.name)))
      .toSeq
      .sortWith((a, b) => a.repositories.lengthCompare(b.repositories.size) > 0)
  }

  /**
    * Get a single repository to display.
    *
    * @param name the repository name i.e. gradle rootProject project
    * @return the DisplayRepository or Null if not found
    */
  def getRepository(name: String): DisplayRepository = {
    if (repositories.isEmpty) {
      fetchRepositories()
    }
    repositories.find(name == _.name)
      .map(DisplayRepository(_, mergedValues, mvnValues, pluginValues))
      .orNull
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
      val dependency = DisplayDependency(name, mvnValues.getOrElse(name, null))
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
      val plugin = DisplayPlugin(pluginId, pluginValues.getOrElse(pluginId, null))
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
    mergedValues = Map.empty[String, String]
    mvnValues = Map.empty[String, String]
    pluginValues = Map.empty[String, String]
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
      .map(parseDirectory(_, groupFolder.getName))
      .filter(_ != null)
      .filter(repo => repo.versions.nonEmpty || repo.plugins.nonEmpty)

  }

  private def parseDirectory(projectFolder: File, groupName: String): Repository = {
    if (GradleRepositoryParser.getBuildFile(projectFolder).exists()) {
      Logger.debug(s"gradle project: $projectFolder")
      GradleRepositoryParser.buildRepository(projectFolder, groupName, springBootDefaultData, springBootMasterData)
    } else if (MavenRepositoryParser.getBuildFile(projectFolder).exists()) {
      Logger.debug(s"maven project: $projectFolder")
      MavenRepositoryParser.buildRepository(projectFolder, groupName, springBootDefaultData, springBootMasterData)
    } else if (SBTRepositoryParser.getBuildFile(projectFolder).exists()) {
      Logger.debug(s"sbt project: $projectFolder")
      SBTRepositoryParser.buildRepository(projectFolder, groupName)
    } else null
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
    * Compute project and maven versions.
    */
  private def computeVersions(): Unit = {
    var mvnProjectFutures = Map.empty[String, Future[(String, String)]]
    var mvnCentralFutures = Map.empty[String, Future[(String, String)]]

    repositories.foreach(
      _.versions.foreach(v => {
        val other = mergedValues.get(v._1)
        if (other.isEmpty) {
          mergedValues += v._1 -> v._2
        } else if (VersionComparator.versionCompare(mergedValues(v._1), v._2) < 0) {
          mergedValues += v._1 -> v._2
        }
        if (mvnProjectFutures.get(v._1).isEmpty) {
          mvnProjectFutures += v._1 -> fetcher.getLatestMvnVersion(v._1, localMavenUrl, localMavenUser, localMavenPassword)
        }
        if (mvnCentralFutures.get(v._1).isEmpty) {
          mvnCentralFutures += v._1 -> fetcher.getLatestMvnVersion(v._1, centralMavenUrl, centralMavenUser, centralMavenPassword)
        }
      })
    )
    val sequence = Future.sequence(mvnCentralFutures.values ++ mvnProjectFutures.values)

    // waiting for all the futures
    val list = Await.result(sequence, Duration.Inf)

    list.foreach { element =>
      if (!mvnValues.keySet.contains(element._1) || VersionComparator.versionCompare(element._2, mvnValues(element._1)) > 0) {
        mvnValues += element._1 -> element._2
      }
    }
  }

  /**
    * Compute project and maven plugin versions.
    */
  private def computeProjectPlugins(): Unit = {

    repositories.foreach(
      _.plugins.foreach(v => {
        val other = pluginValues.get(v._1)
        if (other.isEmpty) {
          pluginValues += v._1 -> v._2
        } else if (VersionComparator.versionCompare(pluginValues(v._1), v._2) < 0) {
          pluginValues += v._1 -> v._2
        }
      })
    )
  }

  private def computeSpringBootData(): Unit = {
    springBootDefaultData = springBootVersionService.computeSpringBootData(false)
    springBootMasterData = springBootVersionService.computeSpringBootData(true)
  }

}
