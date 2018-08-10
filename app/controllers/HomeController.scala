package controllers

import javax.inject.Inject

import play.api.ConfigLoader.stringLoader
import play.api.mvc._
import play.api.{Configuration, Logger}
import services.{GitRepositoryService, VersionService}

/**
  * Main controller of repositories.
  */
class HomeController @Inject()(versionService: VersionService, gitRepositoryService: GitRepositoryService,
                               configuration: Configuration) extends InjectedController {

  def index = Action {
    val names = versionService.listProjects()
    Ok(views.html.home(names))
  }

  def clearCache = Action {
    Logger.info("Update Git Repositories")
    gitRepositoryService.updateGitRepositories()
    Logger.info("Update repositories cache")
    versionService.fetchRepositories()
    Redirect(configuration.get("play.http.context"))
  }


}
