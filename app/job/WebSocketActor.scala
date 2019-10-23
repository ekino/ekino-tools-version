package job

import akka.actor.{Actor, ActorRef, PoisonPill, Props}

class WebSocketActor(out: ActorRef) extends Actor {

  override def receive: PartialFunction[Any, Unit] = {
    case SuccessMessage => self ! PoisonPill
    case _: String      => out ! UpdateWithResponseMessage
  }
}

object WebSocketActor {
  def props(out: ActorRef) = Props(new WebSocketActor(out))
}
