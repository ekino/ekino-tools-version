package services

import java.io.File

import javax.inject.{Inject, Singleton}
import model._
import play.api.{Configuration, Logger}
import utils._

/**
  * Main versions service.
  */
@Singleton
class VersionService @Inject()(configuration: Configuration,
                               gitRepositoryService: GitRepositoryService,
                               springBootVersionService: SpringBootVersionService) {

  val springBootDefaultData: SpringBootData = springBootVersionService.computeSpringBootData(false)
  val springBootMasterData: SpringBootData = springBootVersionService.computeSpringBootData(true)

  private val config = Config(configuration)
  private val parsers = Seq(NPMRepositoryParser, SBTRepositoryParser, MavenRepositoryParser, GradleRepositoryParser)
  private val logger = Logger(classOf[VersionService])

  @volatile private var data: RepositoryData = RepositoryData.noData

  /**
    * List all the projects to display.
    *
    * @return a list of Project
    */
  def listProjects(): Seq[Project] = {
    data.repositories
      .map(DisplayRepository(_, data.localDependencies, data.centralDependencies, data.localPlugins, data.gradlePlugins))
      .groupBy(_.project)
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
    * @return an option of the AbstractDisplay
    */
  def getDependency(name: String): Option[AbstractDisplay] = {
    data.getPluginsAndDependencies.find(_.name == name)
  }

  /**
    * Get a single plugin to display.
    *
    * @param pluginId the plugin id ex. com.ekino.base
    * @return the DisplayPlugin or a new one if not found
    */
  def getPlugin(pluginId: String): Option[DisplayPlugin] = {
    data.plugins.find(_.name == pluginId)
  }

  def allDependencies(): Seq[AbstractDisplay] = data.getPluginsAndDependencies.sortBy(_.name)

  def noData: Boolean = data == RepositoryData.noData

  /**
    * Initialize the data if needed.
    */
  def initData(): Unit = {
    val start = System.currentTimeMillis
    data = fetchRepositories()
    logger.info(s"data has been initialized in ${System.currentTimeMillis - start} ms")
  }

  /**
    * Fetch repositories data as repository list, versions, dependencies ...
    */
  def fetchRepositories(): RepositoryData = {
    logger.info("Processing... this will take few seconds")

    val workspace: File = new File(config.filePath)
    if (!workspace.exists) {
      gitRepositoryService.updateGitRepositories()
    }

    val repositories = findRepositories(workspace)
    val (localDependencies, centralDependencies) = computeDependencyVersions(repositories)
    val (localPlugins, gradlePlugins) = computePluginVersions(repositories)
    val plugins = computePlugins(repositories, localPlugins)
    val dependencies = computeDependencies(repositories, centralDependencies)

    RepositoryData(repositories, dependencies, plugins, localDependencies, centralDependencies, localPlugins, gradlePlugins)
  }

  /**
    * Walk the tree to find the repositories
    *
    * @param directory the current directory
    * @return the repositories
    */
  private def findRepositories(directory: File): Seq[Repository] = {
    val files = directory.listFiles
    if (files.exists(_.isFile)) {
      parseRepository(directory).toSeq
    } else {
      files
        .flatMap(findRepositories)
        .toSeq
    }
  }

  private def parseRepository(projectFolder: File): Option[Repository] = {
    parsers
      .filter(_.canProcess(projectFolder))
      .flatMap(_.buildRepository(projectFolder, projectFolder.getParentFile.getName, springBootDefaultData, springBootMasterData))
      .reduceOption((r1, r2) => Repository(
        r1.name,
        r1.group,
        r1.dependencies ++ r2.dependencies,
        r1.toolVersion + "/" + r2.toolVersion,
        r1.plugins ++ r2.plugins
      ))
      .filter(repo => repo.dependencies.nonEmpty || repo.plugins.nonEmpty)
  }

  /**
    * Compute local plugin versions.
    */
  private def computePluginVersions(repositories: Seq[Repository]): (Map[String, String], Map[String, String]) = {
    val plugins = repositories
      .flatMap(_.plugins)
      .groupBy(_.name)
      .mapValues(seq => seq.map(_.version).max(VersionComparator))

    val localPluginFutures = plugins.keys.map(p => MavenVersionFetcher.getLatestVersion(getPluginCoordinates(p), config.mavenLocalPlugins))
    val gradlePluginFutures = plugins.keys.map(p => MavenVersionFetcher.getLatestVersion(getPluginCoordinates(p), config.mavenGradlePlugins))

    val list = FutureHelper.await[(String, String)](localPluginFutures ++ gradlePluginFutures, configuration, "timeout.compute-plugins")

    val result = list
      .groupBy(_._1)
      .map(p => getPluginId(p._1) -> p._2.map(_._2).max(VersionComparator))

    (plugins, result)
  }

  private def getPluginCoordinates(pluginId: String): String = {
    if (pluginId.contains(':')) {
      pluginId
    } else {
      s"$pluginId:$pluginId.gradle.plugin"
    }
  }

  private def getPluginId(pluginCoordinates: String): String = {
    if (pluginCoordinates.endsWith(".gradle.plugin")) {
      pluginCoordinates.split(':')(0)
    } else {
      pluginCoordinates
    }
  }

  /**
    * Compute local and central versions.
    */
  private def computeDependencyVersions(repositories: Seq[Repository]): (Map[String, String], Map[String, String]) = {
    val dependencies = repositories
      .flatMap(_.dependencies)
      .groupBy(_.name)
      .mapValues(seq => seq.map(_.version).max(VersionComparator))

    val localDependencyFutures = dependencies.keys.filter(MavenVersionFetcher.isMavenVersion).map(MavenVersionFetcher.getLatestVersion(_, config.mavenLocal))
    val centralDependencyFutures = dependencies.keys.filter(MavenVersionFetcher.isMavenVersion).map(MavenVersionFetcher.getLatestVersion(_, config.mavenCentral))
    val npmDependencyFutures = dependencies.keys.filter(!MavenVersionFetcher.isMavenVersion(_)).map(NpmVersionFetcher.getLatestVersion(_, config.npmRegistry))

    val list = FutureHelper.await[(String, String)](centralDependencyFutures ++ localDependencyFutures ++ npmDependencyFutures, configuration, "timeout.compute-plugins")

    val result = list.groupBy(_._1).mapValues(a => a.map(_._2).max(VersionComparator))

    (dependencies, result)
  }

  /**
    * Compute plugin map from repositories
    */
  private def computePlugins(repositories: Seq[Repository], localPlugins: Map[String, String]): Seq[DisplayPlugin] = {
    repositories
      .flatMap(repo => repo.plugins.map((_, repo.name)))                    // Extract all the (plugin, project name) tuples
      .groupBy(_._1.name)                                                   // Group by name
      .mapValues(_.groupBy(_._1.version))                                   // Group by version
      .map(p => DisplayPlugin.from(                                         // Create a DisplayPlugin with:
        repositories.flatMap(_.plugins).find(_.name == p._1).orNull,        // - the plugin name
        localPlugins.getOrElse(p._1, ""),                                   // - its latest released version
        p._2.mapValues(_.map(_._2).toSet)))                                 // - all the projects using it by version
      .toSeq
  }

  /**
    * Compute dependency map from repositories
    */
  private def computeDependencies(repositories: Seq[Repository], centralDependencies: Map[String, String]): Seq[DisplayDependency] = {
    repositories
      .flatMap(repo => repo.dependencies.map((_, repo.name)))                     // Extract all the (dependency, project name) tuples
      .groupBy(_._1.name)                                                         // Group by dependency name
      .mapValues(_.groupBy(_._1.version))                                         // Group by dependency version
      .map(d => DisplayDependency.from(                                           // Create a DisplayDependency with:
        repositories.flatMap(_.dependencies).find(_.name == d._1).orNull,         // - the dependency name
        centralDependencies.getOrElse(d._1, ""),                                  // - its latest released version
        d._2.mapValues(_.map(_._2).toSet)))                                       // - all the projects using it by version
      .toSeq
  }
}
