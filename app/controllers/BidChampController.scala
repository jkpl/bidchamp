package controllers

import javax.inject._

import actors.WebSocketActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services.BidChampCore

import scala.concurrent.Future

@Singleton
class BidChampController @Inject()(
  bidChamp: BidChampCore
)(implicit system: ActorSystem, materializer: Materializer)
    extends Controller {

  def state = Action { Ok("Cool story bro") }

  def socket = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    Future.successful(request.session.get("user") match {
      case None => Left(Forbidden)
      case Some(_) => Right(ActorFlow.actorRef(out => WebSocketActor.props(out, bidChamp.gameActor)))
    })
  }


}
