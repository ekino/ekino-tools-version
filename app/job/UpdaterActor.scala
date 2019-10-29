package job

import akka.actor.Actor
import javax.inject.{Inject, Singleton}
import play.api.Logger
import scalaz.concurrent.Task
import services.{GitRepositoryService, VersionService}

/**
  * Updater Actor that clear the cache.
  */
@Singleton
class UpdaterActor @Inject()(versionService: VersionService, gitRepositoryService: GitRepositoryService) extends Actor {

  private val logger = Logger(classOf[UpdaterActor])

  def receive: PartialFunction[Any, Unit] = {
    case InitMessage               => init().map(_ => sender() ! SuccessMessage).unsafePerformSync
    case UpdateMessage             => update().unsafePerformSync
    case UpdateWithResponseMessage => update().map(_ => sender() ! SuccessMessage).unsafePerformSync
    case StatusMessage             => sender() ! SuccessMessage
    case message                   => logger.error(s"Cannot process message: $message")
  }

  def init(): Task[_] =
    if (versionService.noData) {
      versionService.initData()
    } else {
      Task.now(null)
    }

  def update(): Task[_] = {
    logger.info("Update Git Repositories")
    versionService.initData()
  }
}
