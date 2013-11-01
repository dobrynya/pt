package controllers

import model._
import java.util.Date
import java.text.SimpleDateFormat
import scala.util.Try
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.concurrent.Future
import play.api.libs.iteratee.Input.Empty

case class UserChannel(user: User, channel: Concurrent.Channel[JsValue]) {
  def send(message: JsValue) {
    println("Sending message %s to %s".format(message, user))
    channel.push(message)
  }
}

object Chat extends Controller {
  val sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss")
  var loggedInUsers = Map.empty[String, UserChannel]

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
      val user = Try(Storage.findUserOrCreate(username))
      if (user.isFailure)
        (Iteratee.ignore, Enumerator[JsValue](Json.obj("kind" -> "error",
          "errorMessage" -> "User %s does not exist!".format(username))) >>> Enumerator.eof)
      else {
        val userChannel = UserChannel(user.get, channel)

        val iteratee = Iteratee.foreach[JsValue] {m =>
          println("Received request: " + m)
          if ("history" == (m \ "kind").as[String])
            processHistory((m \ "opponent").as[String], userChannel)
          else if ("delete" == (m \ "kind").as[String]) //
            deleteChat(userChannel, (m \ "opponent").as[String])
          else
            processMessage((m \ "recipient").as[String], (m \ "text").as[String], userChannel)
        } mapDone {_ =>
          println("User %s is disconnected!" format username)
          disconnected(userChannel.user)
        }

        connected(userChannel)

        (iteratee,
          Enumerator[JsValue](Json.obj("kind" -> "connected", "user" -> userChannel.user.username,
            "users" -> loggedInUsers.toList.map(_._2.user.username))) >>> enumerator)
      }
    }
  }

  def broadcast(msg: JsValue) {
    loggedInUsers.foreach {_._2.send(msg)}
  }

  def disconnected(user: User) {
    loggedInUsers -= user.username
    broadcast(Json.obj("kind" -> "disconnected", "user" -> user.username))
  }

  def connected(userChannel: UserChannel) {
    loggedInUsers += userChannel.user.username -> userChannel
    broadcast(Json.obj("kind" -> "connected", "user" -> userChannel.user.username, "users" -> loggedInUsers.toList.map(_._2.user.username)))
  }

  def processHistory(opponent: String, userChannel: UserChannel) {
    println("Processing history of %s and %s".format(userChannel.user.username, opponent))
    try {
      userChannel.send(Json.obj("kind" -> "history", "messages" ->
        Storage.findMessages(userChannel.user, Storage.findUserOrCreate(opponent))
          .map(m => Json.obj("sender" -> m.author, "text" -> m.text, "created" -> sdf.format(m.created)))
      ))
    } catch {
      case th => th.printStackTrace()
    }
  }

  def deleteChat(userChannel: UserChannel, opponent: String) {
    println("Delete chat between %s and %s".format(userChannel.user.username, opponent))
    Storage.deleteChat(userChannel.user, Storage.findUserOrCreate(opponent))
  }

  def processMessage(recipient: String, text: String, sender: UserChannel) {
    println("Processing message from %s to %s".format(sender.user.username, recipient))
    loggedInUsers(recipient)
      .send(Json.obj("kind" -> "message", "sender" -> sender.user.username, "text" -> text, "created" -> sdf.format(new Date)))
    try {
      val opponent = Storage.findUserOrCreate(recipient)
      println("Recipient: " + opponent)
      val chat = Storage.findChatOrCreate(sender.user, opponent)
      println("Chat: " + chat)
      Storage.postMessage(chat, sender.user, text)
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}
