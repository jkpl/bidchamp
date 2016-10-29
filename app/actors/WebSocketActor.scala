package actors

import akka.actor._
import model.BidChampData.{AuthCommand, Command}
import play.api.libs.json.{JsString, JsValue, Reads}


object WebSocketActor {
  def props(out: ActorRef, gameActor: ActorRef) = Props(new WebSocketActor(out, gameActor))
}

case class JsExtractor[T](implicit reads : Reads[T]){
  def unapply(js : JsValue): Option[T] = {
    js.asOpt[T]
  }
}


/**
  * Processes Json and pass commands to change state of game.
  */
class WebSocketActor(out: ActorRef, gameActor : ActorRef) extends Actor {
  val commandExtractor = JsExtractor[Command]()
  val authCommandExtractor = JsExtractor[AuthCommand]()


  def receive = {
    case commandExtractor(command) =>
      println(command)
      out ! JsString("I received your command: " + command.toString)

    case authCommandExtractor(command) =>
      println(command)
      out ! JsString("I received your auth command: " + command.toString)

  }

  override def postStop() = {
  }
}