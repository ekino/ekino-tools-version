package job


import akka.actor.Actor
import javax.inject.{Inject, Singleton}
import play.api.Logger
import services.{GitRepositoryService, VersionService}

/**
  * Updater Actor that clear the cache.
  */
@Singleton
class UpdaterActor @Inject()(versionService: VersionService, gitRepositoryService: GitRepositoryService) extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    case UpdateMessage =>
      update()
      sender() ! SuccessMessage
    case message  =>
      Logger.error(s"Cannot process message: $message")
      sender() ! ErrorMessage
  }

  def update(): Unit = {
    val start = System.currentTimeMillis

    Logger.info("Update Git Repositories")
    gitRepositoryService.updateGitRepositories()
    Logger.info("Update repositories cache")
    versionService.fetchRepositories()

    Logger.info("took " + (System.currentTimeMillis - start) + " ms to get data")
  }
}
