package controllers

import play.api.mvc.InjectedController

/**
  * Health controller for application monitoring.
  */
class HealthController extends InjectedController {

  def index = Action {
    Ok(views.html.health())
  }

}
