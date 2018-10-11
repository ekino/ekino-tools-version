package job

import akka.actor.{ActorRef, ActorSystem}
import javax.inject.{Inject, Named}
import model.CustomExecutionContext.executionContextExecutor
import play.api.ConfigLoader.finiteDurationLoader
import play.api.Configuration

/**
  * Akka Actor Scheduler.
  */
class Scheduler @Inject()(val system: ActorSystem, @Named("updater-actor") val updaterActor: ActorRef, configuration: Configuration) {

  system.scheduler.schedule(
    configuration.get("scheduler.initial-delay"),
    configuration.get("scheduler.interval"),
    updaterActor,
    UpdateMessage)
}
