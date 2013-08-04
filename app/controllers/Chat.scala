package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.concurrent.Future
import play.api.libs.iteratee.Input.Empty

/**
 * @author Dmitry Dobrynin
 *         Created at 01.08.13 21:16
 */
object Chat extends Controller {
  case class User(name: String)
  case class Message(username: String, text: String)

  implicit val user2json = new Writes[User] {
    def writes(o: User) = Json.obj("name" -> o.name)
  }

  var loggedInUsers = Map.empty[String, User]

  def index = Action {implicit request =>
    Ok(views.html.chat())
  }

  def users = Action {implicit request =>
    Ok(Json.toJson(loggedInUsers.toList.map(_._2).sortBy(_.name)))
  }

  def logIn(name: String) = Action {implicit request =>
    if (loggedInUsers.contains(name))
      Ok(Json.obj("result" -> "error", "message" -> ("User %s already logged in!" format name)))
    else {
      val result = Json.obj("result" -> "success", "users" -> Json.toJson(loggedInUsers.toList.map(_._2).sortBy(_.name)))
      loggedInUsers += name -> User(name)
      Ok(result)
    }
  }

  def send = Action(parse.json) {request =>
    println(request.body)
    Ok(Json.obj("result" -> "success"))
  }

  def connectToEcho = WebSocket.using[JsValue] {implicit request =>
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    val iteratee = Iteratee.foreach[JsValue] {e =>
      println(e)
      channel.push(e)
    } mapDone {_ =>
      println("Iteratee done")
    }

    (iteratee, enumerator)
  }
}
