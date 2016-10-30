package controllers

import java.util.UUID
import javax.inject._

import actors.WebSocketActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services.{BidChampCore, UserStore}

import scala.concurrent.Future
import scala.util.Try

@Singleton
class BidChampController @Inject()(
  bidChamp: BidChampCore, val userStore: UserStore
)(implicit system: ActorSystem, materializer: Materializer)
    extends Controller with Authorization {

  val logger: Logger = Logger(this.getClass)

  def state = Action { Ok("Cool story bro") }

  def socket = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    Future.successful {
      val flow = for {
        tokenString <- request.session.get("session-token")
        token <- parseUuid(tokenString)
        userAccount <- userStore.getUserByToken(token)
      } yield ActorFlow.actorRef(out => WebSocketActor.props(userAccount.uuid, out, bidChamp.gameActor))

      flow.toRight(Forbidden)
    }
  }

  def parseUuid(s: String): Option[UUID] = Try(UUID.fromString(s)).toOption
}
