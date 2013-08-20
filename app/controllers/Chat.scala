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

case class Message(recipient: String, text: String)

object Chat extends Controller {
  def index = Action {implicit request =>
    Ok(views.html.chat())
  }

  def logIn(name: String) = WebSocket.using[JsValue] {implicit request =>
    println("received request to log in from user %s!" format name)
    ChatManager.logIn(name)
  }
}

object ChatManager {
  var loggedInUsers = Map.empty[String, User]

  def users = loggedInUsers.toList.map(_._2).sortBy(_.name)

  def logIn(username: String) = {
    if (loggedInUsers.contains(username))
      (Iteratee.ignore[JsValue],
        Enumerator[JsValue](Json.obj("kind" -> "error",
          "errorMessage" -> "User %s already logged in!".format(username))) >>> Enumerator.eof)
    else {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val user = User(username, channel)

      val iteratee = Iteratee.foreach[JsValue] {m =>
        println("Received request: " + m)
        processMessage(Message((m \ "recipient").as[String], (m \ "text").as[String]), user)
      } mapDone {_ =>
        println("User %s is disconnected!" format username)
        ChatManager.disconnected(user)
      }

      connected(user)

      (iteratee, Enumerator[JsValue](Json.obj("kind" -> "connected", "user" -> user.name,
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

  def processMessage(message: Message, sender: User) {
    println("Processing message %s from user %s".format(message, sender.name))
    loggedInUsers(message.recipient).send(Json.obj("kind" -> "message", "sender" -> sender.name, "text" -> message.text))
  }
}
