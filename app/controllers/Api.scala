package controllers

/**
  * Created on 2015-11-13.
  * @author Sina Ghaffari
  */

import java.text.{DecimalFormat, SimpleDateFormat}
import java.util.Calendar

import play.api.cache.Cache
import play.api.libs.json.{Json, JsObject}
import play.api.libs.ws.WS
import play.api.mvc._
import tools.Util.isValidAmount

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Api extends Controller {
  def rates() = Action.async { request =>
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    val timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    val remoteIP = request.remoteAddress
    val body = request.body.asJson.map(_.as[JsObject]).orNull
    val currencies = Await.result(tools.Util.getSupportedCurrencies, Duration.Inf).orNull
    if (currencies == null) {
      Future {
        NotFound(Json.obj("error" -> "List of supported currencies could not be retrieved."))
      }
    } else if(body == null || body.keys.contains("srcAmt") || body.keys.contains("srcCur") || body.keys.contains("srcAmt")) {
      Future {
        BadRequest(Json.obj("error" -> "Request did not contain the correct parametres."))
      }
    }
    val srcAmt = (body \ "srcAmt").validate[String].getOrElse(null)
    val srcCur = (body \ "srcCur").validate[String].getOrElse(null)
    val dstCur = (body \ "dstCur").validate[String].getOrElse(null)
    println("[" + timeFormatter.format(Calendar.getInstance().getTime) + "] Request from " + remoteIP + " with parameters " + "(srcAmt = " + srcAmt + ", srcCur = " + srcCur + ", dstCur = " + dstCur + ")")
    if (!tools.Util.isValidAmount(srcAmt)) {
      Future {
        BadRequest(Json.obj("error" -> "The amount you entered is not a valid decimal value. Please try again."))
      }
    } else if (!currencies.keys.contains(srcCur)) {
      Future {
        BadRequest(Json.obj("error" -> "The requested source currency is not supported. Please try again."))
      }
    } else if (!currencies.keys.contains(dstCur)) {
      Future {
        BadRequest(Json.obj("error" -> "The requested destination currency is not supported. Please try again."))
      }
    } else {
      val lastCall = Cache.getAs[Long]("ratesTime").getOrElse(-1L)

      {
        val currentCache: JsObject = Cache.getAs[JsObject]("rates").orNull
        if (currentCache == null || lastCall == -1 || (System.currentTimeMillis() - lastCall) > 3600000) {
          // Rates have expired.
          if (lastCall != -1) println("    Rates expired " + (System.currentTimeMillis() - lastCall - 3600000) + " ms ago.")
          else println("    Rates were not cached.")
          Cache.set("ratesTime", System.currentTimeMillis())
          WS.url("http://openexchangerates.org/latest.json?app_id=7f09b6b1b34b4642888c99cd87847c3b")
            .get()
            .map { response => response.json \ "rates" }
            .map { t =>
              val newRates: JsObject = t match {
                case a: JsObject => a
                case _ => null
              }
              if (newRates != null)
                Cache.set("rates", newRates)
              newRates
            }
        } else {
          println("    Rates were cached " + (System.currentTimeMillis() - lastCall) + " ms ago.")
          Future(currentCache)
        }
      }.map{allRates =>
        val srcCurVal: BigDecimal = allRates.value.get(srcCur).map(t => BigDecimal(t.toString)).orNull
        val dstCurVal: BigDecimal = allRates.value.get(dstCur).map(t => BigDecimal(t.toString)).orNull
        if(srcCurVal != null && dstCurVal != null) {
          val fmt = new DecimalFormat("0.00########")
          "" + fmt.format(BigDecimal(srcAmt) * dstCurVal / srcCurVal)
        } else {
          "ERR"
        }
      }.map{ dstAmt =>
        Ok(Json.obj(
          "srcCur" -> srcCur,
          "dstCur" -> dstCur,
          "srcAmt" -> srcAmt,
          "dstAmt" -> dstAmt
        ))
      }
    }
  }

  def list() = Action.async {request =>
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    WS.url("http://openexchangerates.org/currencies.json").get()
      .map {response => Ok(response.json)}
  }

  def convert() = Action.async {request =>
    import scala.util.Random
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    Future(Ok(
    request.body.asJson
      .map { body =>
        val srcAmt = (body \ "srcAmt").validate[String].getOrElse(null)
        val srcCur = (body \ "srcCur").validate[String].getOrElse(null)
        val dstAmt = (body \ "dstAmt").validate[String].getOrElse(null)
        val dstCur = (body \ "dstCur").validate[String].getOrElse(null)
        if (srcAmt == null || srcCur == null || dstAmt == null || dstCur == null)
          Json.obj("error" -> "Invalid inputs.")

        val rand = new Random().nextDouble()
        val result = rand < 0.8
        Json.obj("result" -> (if(result)  "success" else "failure"))
      }.orNull))
  }
}
