package controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}
import services.VersionService
import utils.Formatters._

/**
  * Dependency controller.
  */
class DependencyController @Inject()(versionService: VersionService) extends InjectedController {

  def dependency(name: String): Action[AnyContent] = Action { implicit request =>
    val dependency = versionService.getDependency(name)
    dependency match {
      case Some(d) => render {
        case Accepts.Json() => Ok(Json.toJson(d))
        case _              => Ok(views.html.dependency(d))
      }
      case _ => NotFound
    }
  }

  def dependencies(): Action[AnyContent] = Action { implicit request =>
    val dependencies = versionService.allDependencies()
    render {
      case Accepts.Json() => Ok(Json.toJson(dependencies))
      case _              => Ok(views.html.dependencies(dependencies))
    }
  }

}
