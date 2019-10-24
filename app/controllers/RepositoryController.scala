package controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}
import services.VersionService
import utils.SpringBootUtils
import utils.Formatters._
/**
  * Controller of repository details.
  */
class RepositoryController @Inject()(versionService: VersionService) extends InjectedController {

  def index(name: String): Action[AnyContent] = Action { implicit request =>
    versionService.getRepository(name) match {
      case Some(repository) =>
        val springBootData = SpringBootUtils.getSpringBootData(repository.repository.plugins)
        render {
          case Accepts.Json() => Ok(Json.toJson(repository))
          case _              => Ok(views.html.repository(repository, springBootData))
        }
      case None => NotFound
    }
  }
}
