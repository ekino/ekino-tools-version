package controllers

import javax.inject.Inject

import play.api.mvc.InjectedController
import services.VersionService

/**
  * Dependency controller.
  */
class DependencyController @Inject()(versionService: VersionService) extends InjectedController {

  def dependency(name: String) = Action {
    val dependency = versionService.getDependency(name)
    dependency match {
      case Some(d) => Ok(views.html.dependency(d))
      case _       => NotFound
    }
  }

  def dependencies() = Action {
    val dependencies = versionService.allDependencies()
    Ok(views.html.dependencies(dependencies))
  }

}
