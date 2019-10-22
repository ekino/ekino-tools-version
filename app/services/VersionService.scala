package services

import java.io.File

import javax.inject.{Inject, Singleton}
import model.{Dependency, _}
import play.api.{Configuration, Logger}
import scalaz.concurrent.Task
import utils.TaskHelper.gather
import utils._

/**
  * Main versions service.
  */
@Singleton
class VersionService @Inject()(configuration: Configuration,
                               gitRepositoryService: GitRepositoryService,
                               springBootVersionService: SpringBootVersionService) {

  private val config = Config(configuration)
  private val parsers = Seq(YarnLockRepositoryParser, NPMRepositoryParser, SBTRepositoryParser, MavenRepositoryParser, GradleRepositoryParser)
  private val logger = Logger(classOf[VersionService])

  @volatile var data: RepositoryData = RepositoryData.noData

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
  def initData(): Task[_] = {
    val start = System.currentTimeMillis
    fetchRepositories().map(repo => {
      logger.info(s"data has been initialized in ${System.currentTimeMillis - start} ms")
      data = repo
    })
  }

  /**
    * Fetch repositories data as repository list, versions, dependencies ...
    */
  def fetchRepositories(): Task[RepositoryData] = {
    logger.info("Processing... this will take few seconds")

    val workspace: File = new File(config.filePath)
    for {
      _ <- gitRepositoryService.updateGitRepositories()
      springBootDefaultData <- springBootVersionService.computeSpringBootData(false)
      springBootMasterData  <- springBootVersionService.computeSpringBootData(true)
      repositories <- Task.now(findRepositories(workspace, springBootDefaultData, springBootMasterData))
      dependencyVersions <- computeDependencyVersions(repositories)
      pluginVersions <- computePluginVersions(repositories)
      plugins <- computePlugins(repositories, pluginVersions._2)
      dependencies <- computeDependencies(repositories, dependencyVersions._2)
    } yield RepositoryData(
      repositories,
      dependencies,
      plugins,
      dependencyVersions._1,
      dependencyVersions._2,
      pluginVersions._1,
      pluginVersions._2,
      springBootDefaultData,
      springBootMasterData
    )
  }

  /**
    * Walk the tree to find the repositories
    *
    * @param directory the current directory
    * @return the repositories
    */
  private def findRepositories(directory: File, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Seq[Repository] = {
    val files = directory.listFiles
    if (files.exists(_.isFile)) {
      parseRepository(directory, springBootDefaultData, springBootMasterData).toSeq
    } else {
      files
        .flatMap(findRepositories(_, springBootDefaultData, springBootMasterData))
        .toSeq
    }
  }

  private def parseRepository(projectFolder: File, springBootDefaultData: SpringBootData, springBootMasterData: SpringBootData): Option[Repository] = {
    parsers
      .filter(_.canProcess(projectFolder))
      .map(_.buildRepository(projectFolder, projectFolder.getParentFile.getName, springBootDefaultData, springBootMasterData))
      .reduceOption((r1, r2) => r1.copy(
        dependencies = r1.dependencies ++ r2.dependencies,
        toolVersion = r1.toolVersion + "/" + r2.toolVersion,
        plugins = r1.plugins ++ r2.plugins))
      .filter(repo => repo.dependencies.nonEmpty || repo.plugins.nonEmpty)
      .map(r => r.copy(
        dependencies = r.dependencies.sortBy(d => (d.subfolder, d.getType, d.name)),
        plugins = r.plugins.sortBy(p => (p.getType, p.name))))
  }

  /**
    * Compute local plugin versions.
    */
  private def computePluginVersions(repositories: Seq[Repository]): Task[(Map[String, String], Map[String, String])] = {
    val plugins = repositories
      .flatMap(_.plugins)
      .groupBy(_.name)
      .mapValues(seq => seq.map(_.version).max(VersionComparator))

    val pluginVersions = for {
      localPlugins <- gather(plugins.keys.map(p => MavenVersionFetcher.getLatestVersion(getPluginCoordinates(p), config.mavenLocalPlugins)))
      gradlePlugins <- gather(plugins.keys.map(p => MavenVersionFetcher.getLatestVersion(getPluginCoordinates(p), config.mavenGradlePlugins)))
    } yield localPlugins ++ gradlePlugins

    pluginVersions.map(list => (plugins, list.groupBy(_._1).map(p => getPluginId(p._1) -> p._2.map(_._2).max(VersionComparator))))
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
  private def computeDependencyVersions(repositories: Seq[Repository]): Task[(Map[String, String], Map[String, String])] = {
    val dependencyMap: Map[String, Seq[Dependency]] = repositories
      .flatMap(_.dependencies)
      .groupBy(_.name)

    val dependencyVersions = for {
      localDependencies   <- filterDependencyMap(dependencyMap, config.mavenLocal, isJvmDependencies)
      centralDependencies <- filterDependencyMap(dependencyMap, config.mavenCentral, isJvmDependencies)
      npmDependencies     <- filterDependencyMap(dependencyMap, config.npmRegistry, isNodeDependencies, NpmVersionFetcher)
    } yield centralDependencies ++ localDependencies ++ npmDependencies

    dependencyVersions.map(list =>
      (
        dependencyMap.mapValues(seq => seq.map(_.version).max(VersionComparator)),
        list.groupBy(_._1).mapValues(seq => seq.map(_._2).max(VersionComparator))
      )
    )
  }

  /**
    * Compute plugin map from repositories
    */
  private def computePlugins(repositories: Seq[Repository], localPlugins: Map[String, String]): Task[Seq[DisplayPlugin]] = Task {
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
  private def computeDependencies(repositories: Seq[Repository], centralDependencies: Map[String, String]): Task[Seq[DisplayDependency]] = Task {
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

  private def isJvmDependencies(dependencies: Seq[Dependency]) = dependencies.forall(_.isInstanceOf[JvmDependency])
  private def isNodeDependencies(dependencies: Seq[Dependency]) = dependencies.forall(_.isInstanceOf[NodeDependency])
  private def filterDependencyMap(map: Map[String, Seq[Dependency]], site: Site, filters: Seq[Dependency] => Boolean, fetcher: VersionFetcher = MavenVersionFetcher) =
    gather(map.filter(p => filters.apply(p._2)).keys.map(fetcher.getLatestVersion(_, site)))
}
