package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  def index(srcAmtS: String = "", srcCur: String = "", dstCur: String = "") = Action {
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    import play.api.libs.json.JsObject
    import play.api.libs.ws._
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

    // Get list of supported currencies.
    val currenciesResponse = Await.ready(
      WS.url("http://openexchangerates.org/currencies.json")
        .get()
        .map { response => response.json },
      Duration.Inf
    ).value

    if (currenciesResponse.isEmpty) { // Check to see if a response was received.
      NotFound(views.html.index(null, "", "", "", "", "Failed to retrieve currency list."))
    } else { // If a response was given, proceed.
      val resultCurrenciesResponse = currenciesResponse.get
      val currenciesValue = resultCurrenciesResponse.get
      val currencies = currenciesValue match {
        case currencies: JsObject => currencies
        case _ => null
      }
      if (currencies != null) { // If the supported currencies were successfully returned and casted to a JsObject, proceed.
        if ({
          val t = srcAmtS.split('.') // Variable in the scope of the If statement.
          ((t.length == 1 && srcAmtS.forall(_.isDigit)) || (t.length == 2 && t.forall(_.forall(_.isDigit)))) && srcAmtS.length != 0
        }) { // If the entered amount is a valid double, then proceed.
          if (currencies.keys.contains(srcCur) && currencies.keys.contains(dstCur)) { // If the entered currencies are in the supported list.
            // Make the API call to get the most recent rates. Still Blocking *for now*
            // TODO: Prevent rates API call from blocking.
            val ratesResponse = Await.ready(
              WS.url("http://openexchangerates.org/latest.json?app_id=7f09b6b1b34b4642888c99cd87847c3b")
                .get()
                .map { response => response.json \ "rates" },
              Duration.Inf
            ).value

            if (ratesResponse.isEmpty) { // Check to see if a result was received.
              NotFound(views.html.index(currencies, srcAmtS, "", "", "", "Failed to retrieve latest exchange rates."))
            } else {
              val result = ratesResponse.get.get
              val srcAmt = BigDecimal(srcAmtS)
              val latestRates = result match {
                case latestRates : JsObject => latestRates
                case _ => null
              }
              if (latestRates == null) {
                NotFound(views.html.index(currencies, srcAmtS, "", "", "", "Failed to retrieve latest exchange rates."))
              } else {
                val srcCurVal = BigDecimal(latestRates.value.get(srcCur).get.toString())
                val dstCurVal = BigDecimal(latestRates.value.get(dstCur).get.toString())
                val conversionRate = dstCurVal / srcCurVal
                println("ConversionRate = " + conversionRate + " is a BigDecimal:" + conversionRate.isInstanceOf[BigDecimal])
                Ok(views.html.index(currencies, "" + srcAmt, "" + (srcAmt * conversionRate), srcCur, dstCur, ""))
              }
            }
          } else { // Improper srcCur or dstCur
            BadRequest(views.html.index(currencies, "", "", "", "", "srcCur or dstCur not a supported currency code."))
          }
        } else { // Improper srcAmt.
          BadRequest(views.html.index(currencies, "", "", "", "", "The amount you entered is not a valid decimal value. Try again."))
        }
      } else { // Supported Currencies were not received.
        NotFound(views.html.index(null, "", "", "", "", "Failed to retrieve currency list."))
      }
    }
  }
}