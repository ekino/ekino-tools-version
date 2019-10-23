package controllers

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import javax.inject.{Inject, Named}
import job.{UpdateMessage, UpdateWithResponseMessage, WebSocketActor}
import play.api.ConfigLoader.stringLoader
import play.api.libs.json.Json
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import play.api.{ConfigLoader, Configuration}
import services.VersionService
import utils.Formatters._

/**
  * Main controller of repositories.
  */
class HomeController @Inject()(
  versionService: VersionService,
  configuration: Configuration,
  @Named("updater-actor") val updaterActor: ActorRef
)(implicit system: ActorSystem, mat: Materializer) extends InjectedController {

  /**
    * List all the repositories.
    * @return the home Action
    */
  def index: Action[AnyContent] = Action { implicit request =>
    val names = versionService.listProjects()
    render {
      case Accepts.Json() => Ok(Json.toJson(names))
      case _              => Ok(views.html.home(names))
    }
  }

  /**
    * Clear the cache with a websocket message.
    * @return the result
    */
  def clearCache: WebSocket = WebSocket.accept[String, String] { _ => ActorFlow.actorRef { _ => WebSocketActor.props(updaterActor) } }
}
