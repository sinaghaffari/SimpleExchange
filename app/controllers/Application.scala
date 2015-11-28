package controllers

import java.text.{DecimalFormat, SimpleDateFormat}
import java.util.Calendar

import play.api.cache.Cache
import play.api.libs.json.{Json, JsObject, JsValue}
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Application extends Controller {
  def index(srcAmt: String = "", srcCur: String = "", dstCur: String = "") = Action.async { implicit request =>
    import play.api.Play.current
    import play.api.libs.ws._
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    val url = routes.Application.index().absoluteURL()
    println(url)
    WS.url(url + "api/rates")
      .post(
        Json.obj(
          "srcAmt" -> srcAmt,
          "srcCur" -> srcCur,
          "dstCur" -> dstCur
        )
      ).map { response =>
      if (response.status == 200) {
        val dstAmt = (response.json \ "dstAmt").validate[String].getOrElse(null)
        val currencies = Await.result(tools.Util.getSupportedCurrencies, Duration.Inf).get
        Ok(views.html.index(currencies,
          srcAmt,
          dstAmt,
          srcCur,
          dstCur, ""))
      } else {
        println(response.body)
        val error = (response.json \ "error").validate[String].getOrElse(null)
        Ok(views.html.index(null, "", "", "", "", error))
      }
    }
  }
}
