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
    case InitMessage =>
      versionService.initData()
      sender() ! SuccessMessage
    case UpdateMessage =>
      update()
    case UpdateWithResponseMessage =>
      update()
      sender() ! SuccessMessage
    case message  =>
      Logger.error(s"Cannot process message: $message")
  }

  def update(): Unit = {
    val start = System.currentTimeMillis

    Logger.info("Update Git Repositories")
    gitRepositoryService.updateGitRepositories()
    Logger.info("Update repositories cache")
    versionService.initData()

    Logger.info("took " + (System.currentTimeMillis - start) + " ms to create cache")
  }
}
