package controllers


import java.util.UUID
import javax.inject.{Inject, Singleton}

import model.{UserAccount, UserRegistration, UserUpdate}
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Try
import model.UserAccount._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import services.UserStore

@Singleton
class AuthController @Inject() (val userStore: UserStore)

  extends Controller with Authorization {

  val logger: Logger = Logger(this.getClass)

  val maxLoginAttempt = 5

  private val SessionTokenCookieName = "session-token"
  private val cookieDomain = Some("localhost")

  case class LoginCredentials(username: String, password: String)

  implicit val fmtLoginCredentials = Json.format[LoginCredentials]

  private val LoginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginCredentials.apply)(LoginCredentials.unapply)
  )


  def login() = Action(
    Ok(views.html.login())
  )

  def loginPost() = Action{ implicit request =>
    LoginForm.bindFromRequest.fold(
      _ =>  BadRequest(views.html.login(showFailureAlert = true)),
      loginCredentials =>
        userStore.loginUser(loginCredentials.username, loginCredentials.password).map { token =>
          Ok(views.html.index("Happy days!"))
        }.getOrElse(NotFound)
    )
  }


  def createUserAccount() = Action.async(parse.json) { implicit request =>
    println(request.body)
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
        userReg.email,
        userReg.email,
        userReg.name,
        "http://i2.kym-cdn.com/entries/icons/original/000/000/091/cancer.png"
      ))
    }

    validate.toOption.map(account => Future.successful(Ok(Json.toJson(account.uuid)))).getOrElse(Future.successful(BadRequest))
  }

//  def login() = Action.async(parse.json) { implicit request =>
//
//    val parse = request.body
//      .validate[LoginCredentials]
//      .fold(
//        errors => {
//          val errMsg = "/login - unable to parse request body" + errors.mkString(", ")
//          logger.error(errMsg)
//          Try(throw new RuntimeException(errMsg))
//        },
//        valid => Try(valid)
//      )
//
//
//    val validate = parse.toOption flatMap { loginCredentials: LoginCredentials =>
//
//      userStore.validPassword(loginCredentials.username, loginCredentials.password).map { token =>
//        Future.successful {
//          Ok.withCookies(
//            Cookie(
//              name = SessionTokenCookieName,
//              value = token.toString,
//              domain = cookieDomain,
//              maxAge = Some(86400),
//              secure = true
//            )
//          )
//        }
//      }
//    }
//      validate.getOrElse(Future.successful(BadRequest))
//  }

  def logout() = Action.async { implicit request =>
    Future.successful(Ok)
  }

  def getUser() = withUser { request =>
    logger.info(s"/user - request received")

    Ok(Json.toJson(userStore.getUserByToken(UUID.fromString(request.sessionToken))))
  }

  def listUsers() = Action {
    Ok(Json.toJson(userStore.listUsers().map(_.email)))
  }

  def updateAccount() = withUser(parse.json) {
    implicit request =>
      val parse = request.body
        .validate[UserUpdate]
        .fold(
          errors => {
            val errMsg = "/updateUser - unable to parse request body" + errors.mkString(", ")
            logger.error(errMsg)
            Try(throw new RuntimeException(errMsg))
          },
          valid => Try(valid)
        )

      val validate = parse.toOption.map { userUpdate: UserUpdate =>

        val updatedAccount = request.userAccount.copy(name = userUpdate.name)
        userStore.upsertUser(updatedAccount)

            Created
      }
      validate.getOrElse(BadRequest)
  }
}

trait Authorization extends Results {

  private val SessionTokenCookieName = "session-token"

  protected[this] val userStore: UserStore

  class SessionTokenRequest[+A](val sessionToken: String, request: Request[A]) extends WrappedRequest[A](request)

  class UserSessionRequest[+A](sessionToken: String, val userAccount: UserAccount, request: Request[A])
    extends SessionTokenRequest[A](sessionToken, request)

  def readSessionTokenOrElse(ifEmpty: RequestHeader => Result) =  new ActionBuilder[SessionTokenRequest] with ActionRefiner[Request, SessionTokenRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, SessionTokenRequest[A]]] = Future.successful(
      request.session
        .get(SessionTokenCookieName)
        .map(sessionTokenCookie => new SessionTokenRequest(sessionTokenCookie, request))
        .toRight(ifEmpty(request))
    )
  }

  def validateUserSessionOrElse(ifUnauthenticated: RequestHeader => Result): ActionBuilder[UserSessionRequest] =
    readSessionTokenOrElse(ifUnauthenticated) andThen new ActionRefiner[SessionTokenRequest, UserSessionRequest] {
      def refine[A](request: SessionTokenRequest[A]): Future[Either[Result, UserSessionRequest[A]]] = Future.successful{
        val userSessionRequest: Option[UserSessionRequest[A]] =
         userStore.getUserByToken(UUID.fromString(request.sessionToken))
            .map(acc => new UserSessionRequest(request.sessionToken, acc, request))
        userSessionRequest.toRight(ifUnauthenticated(request))
      }

    }

  def withUser = validateUserSessionOrElse { _ =>
    println("redirect to signin")
    Redirect(routes.GithubAuthController.signin())
  }

}