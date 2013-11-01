package model

import java.util.Date
import scala.util.{Failure, Success, Try}
import scala.concurrent._
import ExecutionContext.Implicits._
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.utils.UUIDs

case class User(username: String, name: String)

case class Chat(id: String, created: Date = new Date)

case class Message(author: String, text: String, created: Date)

trait Storage {
  def findUserOrCreate(username: String): User

  def findChatOrCreate(u1: User, u2: User): Chat

  def findMessages(u1: User, u2: User): Iterable[Message]

  def deleteChat(u1: User, u2: User)

  def postMessage(chat: Chat, author: User, text: String)

  def chatId(u1: User, u2: User) = List(u1.username, u2.username).sorted.mkString("-")

  def shutdown
}

object Storage extends Storage {
  var storage: Storage = _

  future(new CassandraStorage).onComplete {
    case Success(s) => storage = s
    case Failure(th) =>
      th.printStackTrace()
      storage = new FakeStorage
  }

  def findUserOrCreate(username: String) = storage.findUserOrCreate(username)

  def findChatOrCreate(u1: User, u2: User) = storage.findChatOrCreate(u1, u2)

  def findMessages(u1: User, u2: User) = storage.findMessages(u1, u2)

  def postMessage(chat: Chat, author: User, text: String) = storage.postMessage(chat, author, text)

  def deleteChat(u1: User, u2: User) = storage.deleteChat(u1, u2)

  def shutdown = storage.shutdown
}

