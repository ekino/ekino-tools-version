package controllers

import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * Health controller for application monitoring.
  */
class HealthController extends InjectedController {

  def index: Action[AnyContent] = Action {
    Ok(views.html.health())
  }

}
