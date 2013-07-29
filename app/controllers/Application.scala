package controllers

import play.api.mvc._
import play.api.mvc.Results.Ok

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
}