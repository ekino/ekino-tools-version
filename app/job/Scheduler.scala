package job

import akka.actor.{ActorRef, ActorSystem}
import javax.inject.{Inject, Named}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Akka Actor Scheduler.
  */
class Scheduler @Inject()(val system: ActorSystem, @Named("scheduler-actor") val schedulerActor: ActorRef)() {
  system.scheduler.schedule(2 minutes, 1 days, schedulerActor, "update")
}
