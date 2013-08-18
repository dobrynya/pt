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
  implicit val user2json = new Writes[User] {
    def writes(o: User) = Json.obj("name" -> o.name)
  }

  def index = Action {implicit request =>
    Ok(views.html.chat())
  }

  def logIn(name: String) = WebSocket.using[JsValue] {implicit request =>
    println("received request to log in from user %s!" format name)
    ChatManager.logIn(name)
  }
}

object ChatManager extends Runnable {
  var loggedInUsers = Map.empty[String, User]

  def users = loggedInUsers.toList.map(_._2).sortBy(_.name)

  def logIn(username: String) = {
    if (loggedInUsers.contains(username))
      throw new IllegalArgumentException("User %s already logged in!" format username)
    else {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val user = User(username, channel)

      val iteratee = Iteratee.foreach[JsValue] {m =>
        (m \ "kind").asOpt[String] match {
          case Some("status") =>
            println("Status requested by user %s" format user.name)
            user.send(Json.obj("kind" -> "connected", "user" -> user.name, "users" -> loggedInUsers.toList.map(_._2.name)))
          case _ =>
            println("Received request: " + m)
            processMessage(Message((m \ "recipient").as[String], (m \ "text").as[String]), user)
        }
      } mapDone {_ =>
        println("User %s is disconnected!" format username)
        ChatManager.disconnected(user)
      }

      loggedInUsers += username -> user
      connected(user)

      (iteratee, enumerator)
    }
  }

  def broadcast(msg: JsValue) {
    loggedInUsers.foreach {
      case (name, u) => u.send(msg)
    }
  }

  def disconnected(user: User) {
    loggedInUsers -= user.name
    broadcast(Json.obj("kind" -> "disconnected", "user" -> user.name))
  }

  def connected(user: User) {
    broadcast(Json.obj("kind" -> "connected", "user" -> user.name, "users" -> loggedInUsers.toList.map(_._2.name)))
  }

  def processMessage(message: Message, sender: User) {
    println("Processing message %s from user %s".format(message, sender.name))
    loggedInUsers(message.recipient).send(Json.obj("kind" -> "message", "sender" -> sender.name, "text" -> message.text))
  }
}
