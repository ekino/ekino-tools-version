package controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import javax.inject.{Inject, Named}
import job.WebSocketActor
import model.RepositoryData
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.streams.ActorFlow
import play.api.mvc._
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
    * Allows to share messages using websockets.
    * @return the message result
    */
  def websocket: WebSocket = WebSocket.accept[String, String] { _ => ActorFlow.actorRef { _ => WebSocketActor.props(updaterActor) } }

  /**
    * Checks that the data is initialized.
    * @return true or false as json
    */
  def initialized: Action[AnyContent] = Action {
    Ok(Json.toJson(!versionService.noData))
  }
}
