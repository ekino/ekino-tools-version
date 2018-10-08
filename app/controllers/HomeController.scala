package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import javax.inject.{Inject, Named}
import job.UpdateMessage
import model.CustomExecutionContext._
import play.api.ConfigLoader.stringLoader
import play.api.mvc._
import play.api.{ConfigLoader, Configuration}
import services.{GitRepositoryService, VersionService}

/**
  * Main controller of repositories.
  */
class HomeController @Inject()(versionService: VersionService, gitRepositoryService: GitRepositoryService,
                               configuration: Configuration, @Named("updater-actor") val updaterActor: ActorRef) extends InjectedController {

  /**
    * List all the repositories.
    * @return the home Action
    */
  def index = Action {
    val names = versionService.listProjects()
    Ok(views.html.home(names))
  }

  /**
    * Clear the cache with an asynchronous action.
    * @return the home Action (redirect)
    */
  def clearCache: Action[AnyContent] = Action.async {
    val timeout = configuration.get("timeout.clear-cache")(ConfigLoader.finiteDurationLoader)
    ask(updaterActor, UpdateMessage)(Timeout(timeout))
      .map(_ => Redirect(configuration.get("play.http.context")))
  }


}
