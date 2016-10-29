package actors

import akka.actor._
import model.{Game, Item}

object GameActor {
  def props() = Props(new GameActor())
}

/**
  * Olds the state of the application / game
  */
class GameActor() extends Actor {

  def state(game : Game) : Receive = {
    case "get" => sender() ! game
  }


  override def receive: Receive = state(Game.newInstance(Item("car", 100)))

  override def postStop() = {}
}
