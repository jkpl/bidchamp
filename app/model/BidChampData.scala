package model

import java.util.UUID
import play.api.libs.json._

case class BidChampData(
  items: Map[String, ItemData] = Map.empty,
  users: Map[UUID, UserData] = Map.empty,
  games: Map[Long, Game] = Map.empty,
  charity: Long = 0
) {
  import BidChampData._

  private def nextGameId: Long =
    if (games.isEmpty) 0L
    else games.keySet.max + 1

  def userStates: List[UserState] =
    users.values.toList.map(userDataToState)

  def userStateForId(userId: UUID): Option[UserState] =
    users.get(userId).map(userDataToState)

  private def userDataToState(data: UserData): UserState = {
    val userGames = for {
      game <- games.values
      if game.bids.contains(data.id)
    } yield UserGameState.fromUserData(data, game)

    UserState(data.id, userGames.toList)
  }

  def eval(command: InternalCommand): Result = command match {
    case Refresh =>
      val updates = games.mapValues { game =>
        val updatedGame = game.updateStatus()
        val events = gameUpdateToEvents(game, updatedGame)
        (game, updatedGame, events)
      }

      val updatedState =updates.foldLeft(this) {
        case (state, (key, (oldGame, updatedGame, _))) =>
          if (hasGameFinished(oldGame, updatedGame))
            state.updateWin(updatedGame)
          else
            state.updateGame(updatedGame)
      }

      val events = updates.values.toList.flatMap(_._3)

      Result(updatedState, events)

    case AddUser(userId) =>
      if (users.values.exists(_.id == userId))
        justEvents(Event(Set.empty, EventContent(s"User '$userId' already exists.", None)))
      else {
        val userData = UserData(userId)
        Result(
          state = copy(users = users + (userData.id -> userData)),
          events = List(Event(Set.empty, EventContent(s"User '$userId' created", None)))
        )
      }

    case UserCommand(userId, c) =>
      users.get(userId) match {
        case None => justEvents(Event(Set.empty, EventContent(s"Invalid user.", None)))
        case Some(userData) =>
          evalWithUser(userData.id, c)
      }
  }

  def updateWin(game: Game): BidChampData =
    game.status match {
      case finished: Finished if games.contains(game.id) =>
        val winnerSet = finished.winners.toSet

        val updatedUsers = users.mapValues { userData =>
          val itemAdded =
            if (winnerSet(userData.id)) userData.addItem(game.item)
            else userData
          game.bids.get(userData.id)
            .map(bid => itemAdded.increaseSpending(bid))
            .getOrElse(itemAdded)
        }

        val updatedItems = items.get(game.item.name).map(_.increasePurchases(game.itemsToWin))

        addItems(updatedItems)
          .updateGame(game)
          .copy(users = updatedUsers, charity = charity + game.exceedAmount)
      case _ =>
        this
    }

  def addItems(itemData: Iterable[ItemData]) = copy(
    items = items ++ itemData.map(d => d.item.name -> d)
  )

  private def evalWithUser(userId: UUID, command: Command): Result = command match {
    case bid: StartBid =>
      items.get(bid.item) match {
        case None =>
          justEvents(Event(Set.empty, EventContent(s"Item '${bid.item}' doesn't exist.", None)))
        case Some(itemData) =>
          val game = Game.newInstance(nextGameId, itemData.item).upsertBid(userId, bid.amount)
          Result(
            state = updateGame(game),
            events = List(Event(Set(userId), EventContent(s"Created a new bid for item '${itemData.item.name}'.", None)))
          )
      }

    case bid: AddToBid =>
      games.get(bid.game) match {
        case None => justEvents(Event(Set.empty, EventContent(s"No game #${bid.game} found.", None)))
        case Some(game) if game.isActive =>
          Result(
            state = updateGame(game.upsertBid(userId, bid.amount)),
            events = List(Event(
              targets = game.bids.keySet - userId,
              content = EventContent(s"New bid of ${bid.amount} has been added for item ${game.item.toString}.", None)
            ))
          )
        case Some(game) =>
          justEvents(Event(Set.empty, EventContent(s"Can't bid on finished games.", None)))
      }
  }

  private def gameUpdateToEvents(oldGame: Game, updatedGame: Game): List[Event] = {
    val itemName = updatedGame.item.name
    val gameId = updatedGame.id
    val bidders = updatedGame.bids.keySet

    (oldGame.status, updatedGame.status) match {
      case (NotStarted, _: Running) =>
        List(Event(
          targets = bidders,
          content = EventContent(s"The bid for '$itemName' has started! ~10 minutes until winners are drawn!", Some(gameId))
        ))

      case (_: Running, finished: Finished) =>
        val winnerSet = finished.winners.toSet
        val losers = bidders -- winnerSet
        val finishedMsg = s"The draw for '$itemName' has finished!"
        List(
          Event(
            targets = losers,
            content = EventContent(s"$finishedMsg Better luck next time!", Some(gameId))
          ),
          Event(
            targets = winnerSet,
            content = EventContent(s"$finishedMsg You have won!", Some(gameId))
          )
        )

      case (_, _) => List.empty
    }
  }

  private def hasGameFinished(oldGame: Game, updatedGame: Game): Boolean =
    oldGame.isActive && updatedGame.isPassive

  def updateGame(game: Game) = copy(games = games + (game.id -> game))

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

  case class AddUser(userId: UUID) extends InternalCommand

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

case class UserState(
  user: UUID,
  games: List[UserGameState]
)

object UserState {
  implicit val userStateJsonFormat: OFormat[UserState] = Json.format[UserState]
}

case class UserGameState(
  gameId: Long,
  gameStatus: String,
  gameStarted: Option[Long],
  gameEnds: Option[Long],
  chanceOfWinning: Option[Double],
  moneySpent: Option[Int]
)

object UserGameState {
  implicit val userGameStateJsonFormat: OFormat[UserGameState] = Json.format[UserGameState]

  def fromUserData(user: UserData, game: Game): UserGameState = {
    val bid = game.bids.get(user.id)
    val chances = game.winningChancesForUser(user.id)

    UserGameState(game.id, game.status.stringMessage, game.startTime, game.endTime, chances, bid)
  }
}

case class ItemData(item: Item, purchased: Int) {
  def increasePurchases(count: Int): ItemData = copy(purchased = purchased + count)
}

object ItemData {
  implicit val itemDataJsonFormat: OFormat[ItemData] = Json.format[ItemData]
}

case class UserData(id: UUID, spent: Int = 0, itemsWon: List[Item] = Nil) {
  def addItem(item: Item): UserData = copy(itemsWon = item :: itemsWon)

  def increaseSpending(amount: Int) = copy(spent = spent + amount)
}

object UserData {
  implicit val userDataJsonFormat: OFormat[UserData] = Json.format[UserData]
}