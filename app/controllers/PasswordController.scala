package controllers

import javax.inject.Singleton

import play.api.Logger
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

@Singleton
class PasswordController extends Controller {

  val logger: Logger = Logger(this.getClass)

  def newPasswordResetRequest() = Action.async(parse.json) { implicit request =>
    Future.successful(Ok)
  }

  def newPasswordResetVerify() = Action.async(parse.json) { implicit request =>
    Future.successful(Ok)
  }

  def newPasswordReset() = Action.async { implicit request =>
    Future.successful(Ok)
  }

}

