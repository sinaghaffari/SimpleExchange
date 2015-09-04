package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index(srcAmt: Double = 0, dstAmt: Double = 0, srcCur: String = "", dstCur: String = "") = Action {
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    import scala.util.Try
    import play.api.libs.json.{JsValue, JsObject}
    import play.api.libs.ws._
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    var dstAmt_ = dstAmt
    val futureCurrenciesResponse = WS.url("http://openexchangerates.org/currencies.json").get().map { response => response.json }
    val resultCurrenciesResponse : Try[JsValue] = Await.ready(futureCurrenciesResponse, Duration.Inf).value.get
    val currencies = resultCurrenciesResponse.get.asInstanceOf[JsObject]

    val futureResult = WS.url("http://openexchangerates.org/latest.json?app_id=7f09b6b1b34b4642888c99cd87847c3b").get().map { response => response.json }
    val result = Await.ready(futureResult, Duration.Inf).value.get
    val latestResponse = result.get.asInstanceOf[JsObject]
    val latestRates = latestResponse.value.get("rates").get.asInstanceOf[JsObject]
    if (latestRates.keys.contains(srcCur) && latestRates.keys.contains(dstCur)) {
      val srcCurVal = latestRates.value.get(srcCur).get.toString().toDouble
      val dstCurVal = latestRates.value.get(dstCur).get.toString().toDouble
      val conversionRate = dstCurVal / srcCurVal
      dstAmt_ = srcAmt * conversionRate
    }
    Ok(views.html.index(currencies, srcAmt, dstAmt_, srcCur, dstCur))
  }
}