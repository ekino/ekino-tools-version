package controllers

import javax.inject.Inject

import play.api.mvc.InjectedController
import services.VersionService

/**
  * Plugin details.
  */
class PluginController @Inject()(versionService: VersionService) extends InjectedController {

  def index(pluginId: String) = Action {
    val plugin = versionService.getPlugin(pluginId)
    plugin match {
      case Some(p) => Ok(views.html.plugin(p))
      case _       => NotFound
    }
  }

}
