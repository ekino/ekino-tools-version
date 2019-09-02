package controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}
import services.VersionService
import utils.Formatters.displayPluginFormat

/**
  * Plugin details.
  */
class PluginController @Inject()(versionService: VersionService) extends InjectedController {

  def index(pluginId: String): Action[AnyContent] = Action { implicit request =>
    val plugin = versionService.getPlugin(pluginId)
    plugin match {
      case Some(p) => render {
        case Accepts.Json() => Ok(Json.toJson(p))
        case _              => Ok(views.html.plugin(p))
      }
      case _ => NotFound
    }
  }

}
