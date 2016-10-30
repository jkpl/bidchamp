package actors

import java.util.UUID

import akka.actor._
import model.{BidChampData, UserState}
import model.BidChampData.Command
import play.api.libs.json._


object WebSocketActor {
  def props(userId: UUID, out: ActorRef, gameActor: ActorRef) = Props(new WebSocketActor(userId, out, gameActor))
}

case class JsExtractor[T](implicit reads : Reads[T]){
  def unapply(js : JsValue): Option[T] = {
    js.asOpt[T]
  }
}


/**
  * Processes Json and pass commands to change state of game.
  */
class WebSocketActor(userId: UUID, out: ActorRef, gameActor : ActorRef) extends Actor with ActorLogging {
  val commandExtractor = JsExtractor[Command]()

  println(userId)

  def receive = {
    case commandExtractor(command) =>
      log.info("Received command: {}", command)
      gameActor ! BidChampData.UserCommand(userId, command)

    case event: BidChampData.EventContent =>
      log.info("Received event: {}", event)
      out ! Json.toJson(event)

    case userState: UserState =>
      log.info("Received user state: {}", userState)
      out ! Json.toJson(userState)
  }

  override def postStop() = {
    gameActor ! BidChampActor.Unsubscribe
  }

  override def preStart(): Unit = {
    gameActor ! BidChampActor.Subscribe(userId)
  }
}