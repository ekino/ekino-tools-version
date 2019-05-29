package controllers

import javax.inject.Inject
import play.api.mvc.InjectedController
import services.VersionService
import utils.SpringBootUtils

/**
  * Controller of repository details.
  */
class RepositoryController @Inject()(versionService: VersionService) extends InjectedController {

  def index(name: String) = Action {
    versionService.getRepository(name) match {
      case Some(repository) =>
        val springBootData = SpringBootUtils.getSpringBootData(repository.repository.plugins, versionService.springBootDefaultData, versionService.springBootMasterData)
        Ok(views.html.repository(repository, springBootData))
      case None             => NotFound
    }
  }
}
