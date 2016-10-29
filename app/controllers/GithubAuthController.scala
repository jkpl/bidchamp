package controllers

import java.util.UUID
import javax.inject.{Inject, Singleton}

import model.UserAccount
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, _}
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc._
import services.{OAuth2, OAuth2Settings, UserStore}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class GithubAuthController @Inject()(userStore : UserStore)(implicit wsClient : WSClient) {

  val SESSION_TOKEN_KEY = "session-token"

  val GITHUB = new OAuth2[GithubUser](OAuth2Settings(
    "f1b0ed53d3a8a45a046b",
    "a435dd914279dc5b6e21409288e7ab38de89d896",
    "https://github.com/login/oauth/authorize",
    "https://github.com/login/oauth/access_token",
    "https://api.github.com/user",
    "http://localhost:9000/callback"
  ), wsClient){
    def user(body: String): GithubUser = Json.parse(body).validate[GithubUser](githubUserReads).get
  }

  case class GithubUser(
                         login: String,
                         email: String,
                         avatar_url: String,
                         name: String
                       )

  implicit val githubUserReads = (
    (__ \ "login").read[String] and
      (__ \ "email").read[String] and
      (__ \ "avatar_url").read[String] and
      (__ \ "name").read[String]
    )(GithubUser.apply _)

  def signin() = Action { implicit request =>
    Redirect(GITHUB.signIn)
  }

  def signout() = Action { implicit request =>
    request.session.get(SESSION_TOKEN_KEY).foreach { token =>
      userStore.removeToken(UUID.fromString(token))
    }
    Redirect(routes.HomeController.index()).withSession()
  }

  def callback(code: String) = Action.async { implicit request =>
    GITHUB.authenticate(code).map { githubUser =>
      val userMaybe = userStore.getUser(githubUser.email)
      val user = userMaybe.getOrElse(
        UserAccount(
          UUID.randomUUID(),
          githubUser.login,
          githubUser.email,
          githubUser.name,
          githubUser.avatar_url
        )
      )
      val upserted: UserAccount = userStore.upsertUser(user)
      val token = userStore.createSession(upserted.email)
      Redirect(routes.HomeController.index())
        .withSession(SESSION_TOKEN_KEY -> token.toString)
    }
  }

}
