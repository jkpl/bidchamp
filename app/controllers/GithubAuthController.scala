package controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, _}
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc._
import services.{OAuth2, OAuth2Settings}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class GithubAuthController @Inject()(implicit wsClient : WSClient) {

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

  def signin() = Action { Redirect(GITHUB.signIn) }

  def signout() = Action { Redirect(routes.HomeController.index()).withSession() }

  def callback(code: String) = Action.async { implicit request =>
    GITHUB.authenticate(code).map { user =>
      println(user)
      Redirect(routes.HomeController.index()).withSession("login" -> user.login)
    }
  }

}
