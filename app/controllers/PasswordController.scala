package controllers

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.mvc.{Action, Controller}
import services.UserStore

import scala.concurrent.Future

@Singleton
class PasswordController @Inject() (userStore: UserStore) extends Controller {

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

