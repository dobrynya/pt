import play.api._
import model.Storage
import concurrent._
import concurrent.ExecutionContext.Implicits.global

object Global extends GlobalSettings {
  override def beforeStart(app: Application) = println("Initialized storage: " + Storage)
  override def onStop(app: Application) = Storage.shutdown
}
