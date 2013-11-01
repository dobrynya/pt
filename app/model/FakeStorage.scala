package model

class FakeStorage extends Storage {
  def shutdown = println("Shutting down")

  def postMessage(chat: Chat, author: User, text: String) {}

  def findUserOrCreate(username: String) = User(username, "Unknown")

  def findChatOrCreate(u1: User, u2: User) = Chat(chatId(u1, u2))

  def deleteChat(u1: User, u2: User) {}

  def findMessages(u1: User, u2: User) = Seq.empty[Message]

  println("FakeStorage initialized")
}
