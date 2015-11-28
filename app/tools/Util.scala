package tools

import play.api.libs.json.{Json, JsObject}
import play.api.libs.ws.WS
import play.cache.Cache

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created on 2015-11-22.
  * @author Sina Ghaffari
  */
object Util {
  def getSupportedCurrencies: Future[Option[JsObject]] = {
    import play.api.Play.current
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

    WS.url("http://openexchangerates.org/currencies.json").get().map { response => response.json.validate[JsObject].asOpt}
  }
2
  def isValidAmount(n: String): Boolean = {
    val t = n.split('.')
    n.length != 0 && ((t.length == 1 && n.forall(_.isDigit)) || (t.length == 2 && t.forall(_.forall(_.isDigit))))
  }

}
