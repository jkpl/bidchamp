package controllers

import javax.inject._

import actors.WebSocketActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import model.UserUpdate
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
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

  case class Bid(item: String, cash: BigDecimal, currency: String = "LIB")

  implicit val fmtBid = Json.format[Bid]

  def state = Action { Ok("Cool story bro") }

  def socket = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    println(request)
    Future.successful(request.session.get("user") match {
//      case None => Left(Forbidden)
      case x : Any =>
        Right(ActorFlow.actorRef(out => WebSocketActor.props(out, bidChamp.gameActor)))
    })
  }

  def bid() = withUser(parse.json) {
    implicit request =>
    val parse = request.body
      .validate[Bid]
      .fold(
        errors => {
          val errMsg = "/bid - unable to parse request body" + errors.mkString(", ")
          logger.error(errMsg)
          Try(throw new RuntimeException(errMsg))
        },
        valid => Try(valid)
      )

      parse.toOption.map { bid: Bid =>
        bidChamp.gameActor ! bid
        Ok
      }.getOrElse(BadRequest)

  }

}
