package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.concurrent.Future
import play.api.libs.iteratee.Input.Empty

case class User(name: String, channel: Concurrent.Channel[JsValue]) {
  def send(message: JsValue) {
    println("Sending message %s to %s".format(message, this))
    channel.push(message)
  }
}

object Chat extends Controller {
  var loggedInUsers = Map.empty[String, User]

  def index = Action {implicit request =>
    Ok(views.html.chat())
  }

  def logIn(username: String) = WebSocket.using[JsValue] {implicit request =>
    println("received request to log in from user %s!" format username)
    if (loggedInUsers.contains(username))
      (Iteratee.ignore[JsValue],
        Enumerator[JsValue](Json.obj("kind" -> "error",
          "errorMessage" -> "User %s already logged in!".format(username))) >>> Enumerator.eof)
    else {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val user = User(username, channel)

      val iteratee = Iteratee.foreach[JsValue] {m =>
        println("Received request: " + m)
        processMessage((m \ "recipient").as[String], (m \ "text").as[String], user)
      } mapDone {_ =>
        println("User %s is disconnected!" format username)
        disconnected(user)
      }

      connected(user)

      (iteratee,
        Enumerator[JsValue](Json.obj("kind" -> "connected", "user" -> user.name,
        "users" -> loggedInUsers.toList.map(_._2.name))) >>> enumerator)
    }
  }

  def broadcast(msg: JsValue) {
    loggedInUsers.foreach {_._2.send(msg)}
  }

  def disconnected(user: User) {
    loggedInUsers -= user.name
    broadcast(Json.obj("kind" -> "disconnected", "user" -> user.name))
  }

  def connected(user: User) {
    loggedInUsers += user.name -> user
    broadcast(Json.obj("kind" -> "connected", "user" -> user.name, "users" -> loggedInUsers.toList.map(_._2.name)))
  }

  def processMessage(recipient: String, text: String, sender: User) {
    println("Processing message from %s to %s".format(sender.name, recipient))
    loggedInUsers(recipient).send(Json.obj("kind" -> "message", "sender" -> sender.name, "text" -> text))
  }
}
