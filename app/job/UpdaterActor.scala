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

  private val logger = Logger(classOf[UpdaterActor])

  def receive: PartialFunction[Any, Unit] = {
    case InitMessage =>
      init()
      sender() ! SuccessMessage
    case UpdateMessage =>
      update()
    case UpdateWithResponseMessage =>
      update()
      sender() ! SuccessMessage
    case message  =>
      logger.error(s"Cannot process message: $message")
  }

  def init(): Unit = {
    if (versionService.noData) {
      versionService.initData()
    }
  }

  def update(): Unit = {
    val start = System.currentTimeMillis

    logger.info("Update Git Repositories")
    gitRepositoryService.updateGitRepositories()
    logger.info("Update repositories cache")
    versionService.initData()

    logger.info("took " + (System.currentTimeMillis - start) + " ms to create cache")
  }
}
