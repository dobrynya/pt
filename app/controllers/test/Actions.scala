package controllers.test

import play.api.mvc.{Controller, Action}
import play.api.libs.json.{JsValue, Writes, Json}
import play.api.libs.json.Json.JsValueWrapper

/**
 * @author Dmitry Dobrynin
 * Created at 29.07.13 22:12
 */
object Actions extends Controller {
  case class Good(name: String, price: Double, presented: Boolean)

  val goods = Vector(
    Good("Велосипед", 1.5, true),
    Good("Самокат", 2.10, true),
    Good("Руковёрт", 4.11, false),
    Good("Шуроповёрт", 5.98, true),
    Good("Мозгоёб", 6.17, false)
  )

  implicit val good2json = new Writes[Good] {
    def writes(g: Good): JsValue = Json.obj("name" -> g.name, "price" -> g.price, "presented" -> g.presented)
  }

  def index = Action {
    Ok(views.html.myown("dmitry"))
  }

  def text = Action {
    Ok("fuck the world!")
  }

  def allgoods = Action {
    Ok(Json.toJson(goods.toSeq))
  }

  def good(id: String) = Action {
    Ok(Json.toJson(goods(id.toInt)))
  }
}
