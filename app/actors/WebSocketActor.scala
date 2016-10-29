package actors

import akka.actor._
import play.api.libs.json.{JsString, JsValue}

object WebSocketActor {
  def props(out: ActorRef, gameActor: ActorRef) = Props(new WebSocketActor(out, gameActor))
}

/**
  * Processes Json and pass commands to change state of game.
  */
class WebSocketActor(out: ActorRef, gameActor : ActorRef) extends Actor {
  def receive = {
    case msg: JsValue =>
      out ! JsString("I received your message: " + msg)
  }

  override def postStop() = {
  }
}