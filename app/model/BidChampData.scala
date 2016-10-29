package model

import java.util.UUID
import play.api.libs.json._

case class BidChampData(
  items: Map[String, Item] = Map.empty,
  users: Map[UUID, UserData] = Map.empty,
  games: Map[Long, Game] = Map.empty,
  charity: Charity = Charity(0)
) {
  import BidChampData._

  private def nextGameId: Long =
    if (games.isEmpty) 0L
    else games.keySet.max + 1

  def eval(command: InternalCommand): Result = command match {
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

    case UserCommand(userId, c) =>
      users.get(userId) match {
        case None => justEvents(s"No such user '$userId' found.")
        case Some(userData) =>
          evalWithUser(userData.user, c)
      }
  }

  private def evalWithUser(user: User, command: Command): Result = command match {
    case bid: StartBid =>
      items.get(bid.item) match {
        case None =>
          justEvents(s"Item '${bid.item}' doesn't exist.")
        case Some(item) =>
          val game = Game.newInstance(nextGameId, item).upsertBid(user, bid.amount)
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
            List(s"User '${user.name}' bid ${bid.amount} for item ${game.item.toString}.")
          )
        case Some(game) =>
          justEvents(s"Can't bid on finished games.")
      }
  }

  private def gameUpdateToEvents(oldGame: Game, updatedGame: Game): List[Event] = {
    Nil // TODO
  }

  private def updateGame(game: Game) = copy(games = games + (game.id -> game))

  private def justEvents(events: Event*): Result = Result(this, events)
}

object BidChampData {
  type Event = String

  case class Result(state: BidChampData, events: Seq[Event])

  sealed trait InternalCommand

  object Refresh extends InternalCommand

  case class AddUser(user: String) extends InternalCommand

  case class UserCommand(
    userId: UUID,
    command: Command
  ) extends InternalCommand

  sealed trait Command

  object Command {
    implicit val reads: Reads[Command] = new Reads[Command] {
      override def reads(json: JsValue): JsResult[Command] =
        (json \ "command").validate[String].flatMap {
          case "startBid" => (json \ "payload").validate[StartBid]
          case "addToBid" => (json \ "payload").validate[AddToBid]
          case _ => JsError.apply("Command not recognised")
        }
    }
  }

  case class StartBid(item: String, amount: Int) extends Command

  case class AddToBid(game: Long, amount: Int) extends Command

  object StartBid {
    implicit val json: OFormat[StartBid] = Json.format[StartBid]
  }

  object AddToBid {
    implicit val json: OFormat[AddToBid] = Json.format[AddToBid]
  }
}

case class UserData(user: User, spent: Int = 0) {
  def id = user.uuid
}

case class Charity(contributed: Int)