package model

import java.util.UUID

import model.BidChampData._
import play.api.libs.json._

case class BidChampData(items: Map[String, Item] = Map.empty,
                        users: Map[UUID, UserData] = Map.empty,
                        games: Map[Long, Game] = Map.empty,
                        charity: Charity = Charity(0)) {

  private def nextGameId: Long =
    if (games.isEmpty) 0L
    else games.keySet.max + 1

  def evalCommand(command: Command): Result = command match {
    case Refresh =>
      val updates = games.mapValues { game =>
        val updatedGame = game.updateStatus()
        val events = gameUpdateToEvents(game, updatedGame)
        (updatedGame, events)
      }

      val updatedGames = updates.mapValues(_._1)
      val events = updates.values.toList.flatMap(_._2)

      Result(copy(games = updatedGames), events)

    case AddUser(username) =>
      if (users.values.exists(_.user.name == username))
        justEvents(s"User '$username' already exists.")
      else {
        val userData = UserData(User(username))
        Result(
          copy(users = users + (userData.id -> userData)),
          List(s"User '$username' created")
        )
      }

    case authCmd: AuthCommand =>
      users.get(authCmd.userId) match {
        case None => justEvents(s"No such user '${authCmd.userId}' found.")
        case Some(userData) =>
          evalAuthCommand(userData.user, authCmd)
      }
  }

  def evalAuthCommand(user: User, command: AuthCommand): Result =
    command match {
      case bid: StartBid =>
        items.get(bid.item) match {
          case None =>
            justEvents(s"Item '${bid.item}' doesn't exist.")
          case Some(item) =>
            val game =
              Game.newInstance(nextGameId, item).upsertBid(user, bid.amount)
            Result(
              updateGame(game),
              List(s"Added game #${game.id} for item '${item.name}'.")
            )
        }

      case bid: AddToBid =>
        games.get(bid.game) match {
          case None => justEvents(s"No game #${bid.game} found.")
          case Some(game) if game.isActive =>
            Result(
              updateGame(game.upsertBid(user, bid.amount)),
              List(
                s"User '${user.name}' bid ${bid.amount} for item ${game.item.toString}."
              )
            )
          case Some(game) =>
            justEvents(s"Can't bid on finished games.")
        }
    }

  private def gameUpdateToEvents(oldGame: Game,
                                 updatedGame: Game): List[Event] = {
    Nil // TODO
  }

  private def updateGame(game: Game) = copy(games = games + (game.id -> game))

  private def justEvents(events: Event*): Result = Result(this, events)
}

object BidChampData {
  type Event = String

  case class Result(state: BidChampData, events: Seq[Event])

  sealed trait Command

  sealed trait AuthCommand extends Command {
    def userId: UUID
  }

  object Command {
    implicit val reads: Reads[Command] = new Reads[Command] {
      override def reads(json: JsValue): JsResult[Command] =
        (json \ "command").validate[String].flatMap {
          case "addUser" => (json \ "payload").validate[AddUser]
          case _ => JsError.apply("Command not recognised")
        }
    }
  }

  object AddUser {
    implicit val json: OFormat[AddUser] = Json.format[AddUser]
  }

  object Refresh extends Command

  case class AddUser(user: String) extends Command

  case class StartBid(userId: UUID, item: String, amount: Int)
      extends AuthCommand

  case class AddToBid(userId: UUID, game: Long, amount: Int)
      extends AuthCommand

  object AuthCommand {
    implicit val reads: Reads[AuthCommand] = new Reads[AuthCommand] {
      override def reads(json: JsValue): JsResult[AuthCommand] =
        (json \ "command").validate[String].flatMap {
          case "startBid" => (json \ "payload").validate[StartBid]
          case "addToBid" => (json \ "payload").validate[AddToBid]
          case _ => JsError.apply("Command not recognised")
        }
    }
  }

  object StartBid {
    implicit val json: OFormat[StartBid] = Json.format[StartBid]
  }

  object AddToBid {
    implicit val json: OFormat[AddToBid] = Json.format[AddToBid]
  }

  case class UserData(user: User, spent: Int = 0) {
    def id = user.uuid
  }

  case class Charity(contributed: Int)

}
