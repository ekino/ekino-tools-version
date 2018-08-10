package job


import akka.actor.Actor
import javax.inject.{Inject, Singleton}
import play.api.Logger
import services.{GitRepositoryService, VersionService}

/**
  * Scheduler Actor clearing cache.
  */
@Singleton
class SchedulerActor @Inject()(versionService: VersionService, gitRepositoryService: GitRepositoryService) extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    case "update" => update()
  }

  def update(): Unit = {
    Logger.info("Update Git Repositories")
    gitRepositoryService.updateGitRepositories()
    Logger.info("Update repositories cache")
    versionService.fetchRepositories()
  }
}
