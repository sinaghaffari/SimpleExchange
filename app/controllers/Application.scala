package controllers

import java.text.SimpleDateFormat
import java.util.Calendar

import play.api._
import play.api.cache.Cache
import play.api.libs.json.{JsValue, JsObject}
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

object Application extends Controller {
  def index(srcAmtS: String = "", srcCur: String = "", dstCur: String = "") = Action { request =>
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    import play.api.libs.json.JsObject
    import play.api.libs.ws._
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

    val timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    val remoteIP = request.remoteAddress
    println("[" + timeFormatter.format(Calendar.getInstance().getTime) + "] Request from " + remoteIP + " with parameters " + "(srcAmtS = " + srcAmtS + ", srcCur = " + srcCur + ", dstCur = " + dstCur + ")")

    val currencies = getSupportedCurrencies

    if (currencies == null) { // Check to see if a response was received.
      println("[" + timeFormatter.format(Calendar.getInstance().getTime) + "]    -> Currency list was not retrieved")
      NotFound(views.html.index(null, "", "", "", "", "Failed to retrieve currency list."))
    } else { // If a response was given, proceed.
      if ({
        val t = srcAmtS.split('.') // Variable in the scope of the If statement.
        ((t.length == 1 && srcAmtS.forall(_.isDigit)) || (t.length == 2 && t.forall(_.forall(_.isDigit)))) && srcAmtS.length != 0
      }) { // If the entered amount is a valid double, then proceed.
        if (currencies.keys.contains(srcCur) && currencies.keys.contains(dstCur)) { // If the entered currencies are in the supported list.
          // Make the API call to get the most recent rates. Still Blocking *for now*
          // TODO: Prevent rates API call from blocking.
          val latestRates = getLatestRates

          if (latestRates == null) { // Check to see if a result was received.
            println("[" + timeFormatter.format(Calendar.getInstance().getTime) + "]    -> Latest exchange rates were not retrieved")
            NotFound(views.html.index(currencies, srcAmtS, "", "", "", "Failed to retrieve latest exchange rates."))
          } else {
            val srcAmt = BigDecimal(srcAmtS)
            val srcCurVal = {
              val t = latestRates.value.get(srcCur)
              if (t.isEmpty) {
                null
              } else {
                BigDecimal(t.get.toString())
              }
            }
            val dstCurVal = {
              val t = latestRates.value.get(dstCur)
              if (t.isEmpty) {
                null
              } else {
                BigDecimal(t.get.toString())
              }

            }
            if (dstCurVal == null || srcCurVal == null) {
              println("[" + timeFormatter.format(Calendar.getInstance().getTime) + "]    -> Latest exchange rates were retrieved but invalid")
              NotFound(views.html.index(currencies, srcAmtS, "", "", "", "Failed to retrieve latest exchange rates."))
            } else {
              val conversionRate = dstCurVal / srcCurVal
              Ok(views.html.index(currencies, "" + srcAmt, "" + (srcAmt * conversionRate), srcCur, dstCur, ""))
            }

          }
        } else { // Improper srcCur or dstCur
          println("[" + timeFormatter.format(Calendar.getInstance().getTime) + "]    -> srcCur = " + srcCur + " or dstCur = " + dstCur + " are not supported currency codes")
          BadRequest(views.html.index(currencies, "", "", "", "", "srcCur or dstCur are not supported currency codes"))
        }
      } else { // Improper srcAmt.
        println("[" + timeFormatter.format(Calendar.getInstance().getTime) + "]    -> Invalid srcAmt input " + srcAmtS)
        BadRequest(views.html.index(currencies, "", "", "", "", "The amount you entered is not a valid decimal value. Try again."))
      }
    }
  }
  def getSupportedCurrencies : JsObject = {
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    val timeCache = Cache.get("currencyTime")
    val currenciesCache = Cache.get("currencies")

    val lastTimeAny = if(timeCache.isDefined) timeCache.get else null
    val lastTime = lastTimeAny match {
      case lastTime : Long => lastTime
      case _ => -1
    }
    if (!currenciesCache.isDefined || (lastTime != -1 && (System.currentTimeMillis() - lastTime) > 3600000)) {
      if  (!currenciesCache.isDefined) {
        println("Currencies not defined")
      } else {
        println("Currencies expired " + (System.currentTimeMillis() - lastTime - 3600000) + " ms ago")
      }
      val currenciesResponse = Await.ready(
        WS.url("http://openexchangerates.org/currencies.json")
          .get()
          .map { response => response.json },
        Duration.Inf
      ).value

      if (currenciesResponse.isEmpty) {
        null
      } else {
        val currenciesVal = currenciesResponse.get.get
        val currencies = currenciesVal match {
          case currencies: JsObject => currencies
          case _ => null
        }
        if (currencies != null) {
          Cache.set("currencyTime", System.currentTimeMillis())
          Cache.set("currencies", currencies)
        }
        currencies
      }
    } else {
      println("Currencies were cached " + (System.currentTimeMillis() - lastTime) + " ms ago")
      val supportedCurAny = Cache.get("currencies").get
      val currencies = supportedCurAny match {
        case currencies : JsObject => currencies
        case _ => null
      }
      currencies
    }
  }
  def getLatestRates : JsObject = {
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    val timeCache = Cache.get("ratesTime")
    val ratesCache = Cache.get("rates")

    val lastTimeAny = if(timeCache.isDefined) timeCache.get else null
    val lastTime = lastTimeAny match {
      case lastTime : Long => lastTime
      case _ => -1
    }
    if (!ratesCache.isDefined || (lastTime != -1 && (System.currentTimeMillis() - lastTime) > 3600000)) {
      if  (!ratesCache.isDefined) {
        println("Rates not defined")
      } else {
        println("Rates expired " + (System.currentTimeMillis() - lastTime - 3600000) + " ms ago")
      }
      val ratesResponse = Await.ready(
        WS.url("http://openexchangerates.org/latest.json?app_id=7f09b6b1b34b4642888c99cd87847c3b")
          .get()
          .map { response => response.json \ "rates" },
        Duration.Inf
      ).value
      if (ratesResponse.isEmpty) {
        null
      } else {
        val ratesVal = ratesResponse.get.get
        val rates = ratesVal match {
          case currencies: JsObject => currencies
          case _ => null
        }
        if (rates != null) {
          Cache.set("ratesTime", System.currentTimeMillis())
          Cache.set("rates", rates)
        }
        rates
      }
    } else {
      println("Rates were cached " + (System.currentTimeMillis() - lastTime) + " ms ago")
      val ratesAny = Cache.get("rates").get
      val rates = ratesAny match {
        case rates : JsObject => rates
        case _ => null
      }
      rates
    }
  }
  def getLatestRatesTemp : JsObject = {
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    val timeCache = Cache.get("ratesTime")
    val ratesCache = Cache.get("rates")

    val lastTimeAny = if(timeCache.isDefined) timeCache.get else null
    val lastTime = lastTimeAny match {
      case lastTime : Long => lastTime
      case _ => -1
    }
    if (ratesCache.isDefined && (lastTime == -1 || (System.currentTimeMillis() - lastTime) <= 30000)) {
      println("Rates were cached " + (System.currentTimeMillis() - lastTime) + " ms ago")
      val ratesAny = Cache.get("rates").get
      ratesAny match {
        case rates : JsObject => rates
        case _ => null
      }
    } else null
  }
}