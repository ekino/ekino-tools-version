package controllers

import javax.inject.Inject

import play.api.mvc.InjectedController
import services.VersionService

/**
  * Controller of repository details.
  */
class RepositoryController @Inject()(versionService: VersionService) extends InjectedController {

  def index(name: String) = Action {
    val repository = versionService.getRepository(name)
    Ok(views.html.repository(repository))
  }
}
