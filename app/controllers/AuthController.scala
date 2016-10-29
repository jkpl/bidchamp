package controllers


import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.typesafe.config.Config
import model.{UserAccount, UserRegistration}
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.util.Try
import model.UserAccount._
import play.api.libs.json.Json
import services.UserStore

@Singleton
class AuthController @Inject() (userStore: UserStore)
  extends Controller {

  val logger: Logger = Logger(this.getClass)

  val maxLoginAttempt = 5

  def createUserAccount() = Action.async(parse.json) { implicit request =>
    val parse = request.body
      .validate[UserRegistration]
      .fold(
        errors => {
          val errMsg = "/user/account - unable to parse request body" + errors.mkString(", ")
          logger.error(errMsg)
          Try(throw new RuntimeException(errMsg))
        },
        valid => Try(valid)
      )
    val validate = parse map { userReg =>

      userStore.upsertUser(
      UserAccount(
        UUID.randomUUID(),
        Some(userReg.email),
        Some(userReg.name),
        userReg.phoneNumber,
        DateTime.now(),
        Some(userReg.password),
        0
      ))
    }

    validate.toOption.map(account => Future.successful(Ok(Json.toJson(account.uuid)))).getOrElse(Future.successful(BadRequest))
  }

  def login() = Action.async(parse.json) { implicit request =>
    Future.successful(Ok)
  }

  def renew() = Action.async { implicit request =>
    Future.successful(Ok)
  }

  def logout() = Action.async { implicit request =>
    Future.successful(Ok)
  }

  def authenticateUserWithCookie() = Action.async { implicit request =>
    Future.successful(Ok)
  }

  def verify() = Action.async(parse.json) { implicit request =>
    logger.info("/user/verify - receiving a new request")
    Future.successful(Ok)
  }

  def getUser(userUuid: String) = Action.async { request =>
    logger.info(s"/user [userUuid=$userUuid] - request received")

    Future.successful(Ok(Json.toJson(userStore.getUser(UUID.fromString(userUuid)))))
  }
}
