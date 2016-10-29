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
        justEvents(Event(Set.empty, EventContent(s"User '$username' already exists.", None)))
      else {
        val userData = UserData(User(username))
        Result(
          state = copy(users = users + (userData.id -> userData)),
          events = List(Event(Set.empty, EventContent(s"User '$username' created", None)))
        )
      }

    case UserCommand(userId, c) =>
      users.get(userId) match {
        case None => justEvents(Event(Set(userId), EventContent(s"Invalid user.", None)))
        case Some(userData) =>
          evalWithUser(userData.user, c)
      }
  }

  private def evalWithUser(user: User, command: Command): Result = command match {
    case bid: StartBid =>
      items.get(bid.item) match {
        case None =>
          justEvents(Event(Set(user.uuid), EventContent(s"Item '${bid.item}' doesn't exist.", None)))
        case Some(item) =>
          val game = Game.newInstance(nextGameId, item).upsertBid(user, bid.amount)
          Result(
            state = updateGame(game),
            events = List(Event(Set(user.uuid), EventContent(s"Created a new bid for item '${item.name}'.", None)))
          )
      }

    case bid: AddToBid =>
      games.get(bid.game) match {
        case None => justEvents(Event(Set(user.uuid), EventContent(s"No game #${bid.game} found.", None)))
        case Some(game) if game.isActive =>
          Result(
            state = updateGame(game.upsertBid(user, bid.amount)),
            events = List(Event(
              targets = game.bids.keySet.map(_.uuid) - user.uuid,
              content = EventContent(s"User '${user.name}' bid ${bid.amount} for item ${game.item.toString}.", None)
            ))
          )
        case Some(game) =>
          justEvents(Event(Set(user.uuid), EventContent(s"Can't bid on finished games.", None)))
      }
  }

  private def gameUpdateToEvents(oldGame: Game, updatedGame: Game): List[Event] = {
    val itemName = updatedGame.item.name
    val gameId = updatedGame.id
    val bidders = updatedGame.bids.keySet.map(_.uuid)

    (oldGame.status, updatedGame.status) match {
      case (NotStarted, _: Running) =>
        List(Event(
          targets = bidders,
          content = EventContent(s"The bid for '$itemName' has started! ~10 minutes until winners are drawn!", Some(gameId))
        ))

      case (_: Running, Finished(winners)) =>
        val winnerIds = winners.map(_.uuid).toSet
        val losers = bidders -- winnerIds
        val finishedMsg = s"The draw for '$itemName' has finished!"
        List(
          Event(
            targets = losers,
            content = EventContent(s"$finishedMsg Better luck next time!", Some(gameId))
          ),
          Event(
            targets = winnerIds,
            content = EventContent(s"$finishedMsg You have won!", Some(gameId))
          )
        )

      case (_, _) => List.empty
    }
  }

  private def updateGame(game: Game) = copy(games = games + (game.id -> game))

  private def justEvents(events: Event*): Result = Result(this, events)
}

object BidChampData {
  case class Event(
    targets: Set[UUID],
    content: EventContent
  )

  case class EventContent(
    body: String,
    gameId: Option[Long]
  )

  object EventContent {
    implicit val eventContentJsonFormat: OFormat[EventContent] = Json.format[EventContent]
  }

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