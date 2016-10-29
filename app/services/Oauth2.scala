package services

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import play.core.parsers._

import scala.concurrent.Future

case class OAuth2Settings(clientId: String,
                          clientSecret: String,
                          signInUrl: String,
                          accessTokenUrl: String,
                          userInfoUrl: String,
                          redirectUri: String)

abstract class OAuth2[T](settings: OAuth2Settings, wsClient : WSClient) {
  def user(body: String): T

  import settings._

  lazy val signIn = signInUrl + "?client_id=" + clientId + "&redirect_uri=" + redirectUri

  def authenticate(code: String): Future[T] = {
    val url = accessTokenUrl + "?client_id=" + clientId + "&client_secret=" + clientSecret + "&code=" + code

    for {
      accessToken <- wsClient.url(url).get()
      userInfo <- wsClient.url(
          userInfoUrl + "?access_token=" + FormUrlEncodedParser
            .parse(accessToken.body)
            .get("access_token")
            .flatMap(_.headOption)
            .get
        )
        .get()
    } yield {
      user(userInfo.body)
    }
  }
}
