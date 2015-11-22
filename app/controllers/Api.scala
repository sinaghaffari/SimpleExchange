package controllers

/**
  * Created on 2015-11-13.
  * @author Sina Ghaffari
  */
import play.api.mvc._

import scala.concurrent.Future

object Api extends Controller {
  def index(srcAmtS: String = "", srcCur: String = "", dstCur: String = "") = Action.async { request =>
    Future {
      Ok("shit's good!");
    }
  }
}
