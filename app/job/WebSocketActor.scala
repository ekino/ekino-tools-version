package job

import akka.actor.{Actor, ActorRef, PoisonPill, Props}

class WebSocketActor(updater: ActorRef) extends Actor {

  override def receive: PartialFunction[Any, Unit] = {
    case SuccessMessage => self ! PoisonPill
    case "status"       => updater ! StatusMessage
    case "clear"        => updater ! UpdateWithResponseMessage
  }
}

object WebSocketActor {
  def props(updater: ActorRef) = Props(new WebSocketActor(updater))
}
