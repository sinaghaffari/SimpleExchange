package tools

import play.api.libs.json.JsObject
import play.api.libs.ws.WS

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created on 2015-11-22.
  * @author Sina Ghaffari
  */
object Util {
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

  def isValidAmount(n: String): Boolean = {
    val t = n.split('.')
    n.length != 0 && ((t.length == 1 && n.forall(_.isDigit)) || (t.length == 2 && t.forall(_.forall(_.isDigit))))
  }
}
