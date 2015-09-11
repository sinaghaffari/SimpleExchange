package controllers

import java.text.{DecimalFormat, SimpleDateFormat}
import java.util.Calendar

import play.api.cache.Cache
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Application extends Controller {
  def index(srcAmtS: String = "", srcCur: String = "", dstCur: String = "") = Action.async { request =>
    import play.api.Play.current
    import play.api.libs.ws._
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

    val timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    val remoteIP = request.remoteAddress
    println("[" + timeFormatter.format(Calendar.getInstance().getTime) + "] Request from " + remoteIP + " with parameters " + "(srcAmtS = " + srcAmtS + ", srcCur = " + srcCur + ", dstCur = " + dstCur + ")")

    val currencies = getSupportedCurrencies

    if (currencies == null) {
      Future {
        NotFound(views.html.index(null, "", null, "", "", "List of supported currencies could not be retrieved."))
      }
    } else if (!isValidAmount(srcAmtS)) {
      Future {
        BadRequest(views.html.index(null, "", null, "", "", "The amount you entered is not a valid decimal value. Please try again."))
      }
    } else if (!currencies.keys.contains(srcCur)) {
      Future {
        BadRequest(views.html.index(null, "", null, "", "", "The requested source currency is not supported. Please try again."))
      }
    } else if (!currencies.keys.contains(dstCur)) {
      Future {
        BadRequest(views.html.index(null, "", null, "", "", "The requested destination currency is not supported. Please try again."))
      }
    } else {
      val temp1 = Cache.get("ratesTime").getOrElse("-1")
      val lastCall: Long = temp1 match {
        case t: Long => t
        case _ => (-1).toLong
      }
      val cachedRates: JsObject = Cache.get("rates").orNull match {
        case t: JsObject => t
        case _ => null
      }
      if (cachedRates == null || lastCall == -1 || (System.currentTimeMillis() - lastCall) > 3600000) { // Rates have expired.
        if (lastCall != -1) println("    Rates expired " + (System.currentTimeMillis() - lastCall - 3600000) + " ms ago.")
        else println("    Rates were not cached.")
        Cache.set("ratesTime", System.currentTimeMillis())
        WS.url("http://openexchangerates.org/latest.json?app_id=7f09b6b1b34b4642888c99cd87847c3b")
          .get()
          .map { response => response.json \ "rates" }
          .map { t =>
            Ok(views.html.index(currencies, srcAmtS, t match {
              case a: JsObject => a
              case _ => null
            }, srcCur, dstCur, ""))
          }
      } else { // Rates are cached.
        println("    Rates were cached " + (System.currentTimeMillis() - lastCall) + " ms ago.")
        Future {
          Ok(views.html.index(currencies, srcAmtS, cachedRates, srcCur, dstCur, ""))
        }
      }
    }
  }
  def getSupportedCurrencies: JsObject = {
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

    Await.ready(WS.url("http://openexchangerates.org/currencies.json").get().map { response => response.json }, Duration.Inf)
      .value
      .map { a =>
      a.get match {
        case currencies: JsObject => currencies
        case _ => null
      }
    }.orNull
  }

  def isValidAmount(n : String): Boolean = {
    val t = n.split('.')
    n.length != 0 && ((t.length == 1 && n.forall(_.isDigit)) || (t.length == 2 && t.forall(_.forall(_.isDigit))))
  }
}