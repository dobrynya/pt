package model

import java.util.Date
import com.datastax.driver.core._
import collection.JavaConversions._

class CassandraStorage extends Storage {
  private val cluster = new Cluster.Builder().addContactPoint("localhost").build()
  private val session: Session = cluster.connect("msgstorage")

  private val createUser =
    session.prepare("insert into users (username, name) values (?, ?)").enableTracing()

  private val deleteChatQuery = session.prepare("delete from chats where user = ? and opponent = ?")

  private val createChat = session.prepare("insert into chats (user, opponent, chat, created) values (?, ?, ?, ?) if not exists")

  private val findChatQuery = session.prepare("select * from chats where user = ? and opponent = ?")

  private val createMessage = session.prepare("insert into messages (chat, ts, author, msg) values (?, now(), ?, ?)")

  private val chatMessages = session.prepare("select author, msg, dateOf(ts) as created from messages where chat = ?")

  def findUserOrCreate(username: String) =
    session.execute("select * from users where username = ?", username)
      .map(r => User(r.getString("username"), r.getString("name"))).headOption
      .getOrElse(throw new IllegalArgumentException("User %s has been not found!".format(username)))

  def deleteChat(u1: User, u2: User) = session.execute(deleteChatQuery.bind(u1.username, u2.username))

  def findChat(u1: User, u2: User) = {
    session.execute(findChatQuery.bind(u2.username, u1.username)).headOption.getOrElse(createNewChat(u2, u1))
    session.execute(findChatQuery.bind(u1.username, u2.username)).map(r => Chat(r.getString("chat"))).headOption
  }

  def findChatOrCreate(u1: User, u2: User) = findChat(u1, u2) getOrElse createNewChat(u1, u2)

  def createNewChat(u1: User, u2: User) = {
    val chat = Chat(chatId(u1, u2))
    session.execute(createChat.bind(u1.username, u2.username, chat.id, chat.created))
    println("Chat %s for %s and %s is created".format(chat, u1, u2))
    session.execute(createChat.bind(u2.username, u1.username, chat.id, chat.created))
    println("Chat %s for %s and %s is created".format(chat, u2, u1))
    chat
  }

  def postMessage(chat: Chat, author: User, text: String) = {
    session.execute(createMessage.bind(chat.id, author.username, text))
    Message(author.username, text, new Date)
  }

  def findMessages(u1: User, u2: User) =
    findChat(u1, u2).map(ch =>
      for (r <- session.execute(chatMessages.bind(ch.id)) if ch.created.before(r.getDate("created"))) yield
        Message(r.getString("author"), r.getString("msg"), r.getDate(2))
    ) getOrElse Nil



  def shutdown = {
    println("Shutting down Cassandra session...")
    cluster.shutdown()
  }

  def save(user: User) = {
    session.execute(createUser.bind(user.username, user.name))
    user
  }

  println("CassandraStorage is initialized")
}
