package job

import akka.actor.{ActorRef, ActorSystem}
import javax.inject.{Inject, Named}
import play.api.ConfigLoader.finiteDurationLoader
import play.api.Configuration

import scala.concurrent.ExecutionContext

/**
  * Akka Actor Scheduler.
  */
class Scheduler @Inject()(val system: ActorSystem, @Named("updater-actor") val updaterActor: ActorRef, configuration: Configuration)(implicit executionContext: ExecutionContext) {

  system.scheduler.scheduleWithFixedDelay(
    configuration.get("scheduler.initial-delay"),
    configuration.get("scheduler.interval"),
    updaterActor,
    UpdateMessage)
}
