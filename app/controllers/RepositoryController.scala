package controllers

import javax.inject.Inject

import play.api.mvc.InjectedController
import services.VersionService

/**
  * Controller of repository details.
  */
class RepositoryController @Inject()(versionService: VersionService) extends InjectedController {

  def index(name: String) = Action {
    val springBootData = versionService.springBootDefaultData
    versionService.getRepository(name) match {
      case Some(repository) => Ok(views.html.repository(repository, springBootData))
      case None             => NotFound
    }
  }
}
