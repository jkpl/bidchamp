package model

import java.util.UUID

import play.api.libs.json._
import services.MemoryUserStore

case class BidChampData(
  items: Map[String, ItemData] = Map.empty,
  users: Map[UUID, UserData] = Map.empty,
  oldGames: List[Game] = List.empty,
  currentGames: Map[String, Game] = Map.empty,
  charity: Long = 0
) {
  import BidChampData._

  def userStates: List[UserState] =
    users.values.toList.map(userDataToState)

  def userStateForId(userId: UUID): Option[UserState] =
    users.get(userId).map(userDataToState)

  private def userDataToState(data: UserData): UserState = {
    val userGames = for {
      game <- currentGames.values
    } yield UserItem.fromUserData(data, game)

    val userItems = for {
      (itemId, item) <- items
      if !currentGames.contains(itemId)
    } yield UserItem.fromItem(data, item.item)

    UserState(data.id, data.itemsWon, charity, (userGames.toList ++ userItems.toList).sortBy(_.item.name))
  }

  def eval(command: InternalCommand): Result = command match {
    case Refresh =>
      val updates = currentGames.mapValues { game =>
        val updatedGame = game.updateStatus()
        val events = gameUpdateToEvents(game, updatedGame)
        (game, updatedGame, events)
      }

      val updatedState = updates.foldLeft(this) {
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
      case finished: Finished if currentGames.contains(game.id) =>
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
          .archiveGame(game)
          .copy(users = updatedUsers, charity = charity + game.exceedAmount)

      case _ =>
        this
    }

  def addItems(itemData: Iterable[ItemData]) = copy(
    items = items ++ itemData.map(d => d.item.name -> d)
  )

  private def evalWithUser(userId: UUID, command: Command): Result = command match {
    case bid: AddToBid =>
      currentGames.get(bid.item) match {
        case None =>
          items.get(bid.item) match {
            case None =>
              justEvents(Event(Set.empty, EventContent(s"Item '${bid.item}' doesn't exist.", None)))
            case Some(itemData) =>
              val game = Game(itemData.item)
              Result(
                state = updateGame(game.upsertBid(userId, bid.amount)),
                events = List(Event(Set(userId), EventContent(s"Created a new bid for item '${itemData.item.name}'.", Some(itemData.item.name))))
              )
          }

        case Some(game) if game.isActive =>
          val currentAmount = game.bids.getOrElse(userId, 0)
          if ((currentAmount + bid.amount) < game.item.price / 2) {
            Result(
              state = updateGame(game.upsertBid(userId, bid.amount)),
              events = List(Event(
                targets = game.bids.keySet - userId,
                content = EventContent(s"New bid of ${bid.amount} has been added for item ${game.item.name}.", Some(game.item.name))
              ))
            )
          } else {
            Result(updateGame(game), List(Event(Set(userId), EventContent("Can not bet more than 50% of price of item", Some(game.item.name)))))
          }
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
          content = EventContent(s"The bid for '$itemName' has started! ~1 minute until winners are drawn!", Some(gameId))
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

  def updateGame(game: Game) = copy(currentGames = currentGames + (game.id -> game))

  def archiveGame(game: Game) = copy(oldGames = game :: oldGames, currentGames = currentGames - game.id)

  private def justEvents(events: Event*): Result = Result(this, events)
}

object BidChampData {

  def start: BidChampData = {
    val items = List(
      Item("Bicycle", 500, "assets/images/bike.jpg"),
      Item("Macbook", 1200, "assets/images/apple-macbook.jpg"),
      Item("Subaru", 10000, "assets/images/subaru.jpg")
    ).map(item => item.name -> ItemData(item, 0)).toMap

    val sortedUserIds = MemoryUserStore.users.values.toList.sortBy(_.name).map(_.uuid)
    val users = sortedUserIds.map(id => id -> UserData(id)).toMap

    val currentGames = Map(
      "Macbook" -> Game(items("Macbook").item)
        .upsertBid(sortedUserIds(0), 190)
        .upsertBid(sortedUserIds(1), 512)
        .upsertBid(sortedUserIds(2), 370)
        .upsertBid(sortedUserIds(3), 220)
        .updateStatus(),
      "Bicycle" -> Game(items("Bicycle").item).upsertBid(sortedUserIds.head, 102).updateStatus()
    )

    BidChampData(
      items = items,
      users = users,
      currentGames = currentGames
    )
  }

  case class Event(
    targets: Set[UUID],
    content: EventContent
  )

  case class EventContent(
    body: String,
    itemId: Option[String],
    eventType : String = "NOTIFICATION"
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
          case "addToBid" => (json \ "payload").validate[AddToBid]
          case _ => JsError.apply("Command not recognised")
        }
    }
  }

  case class AddToBid(item: String, amount: Int) extends Command

  object AddToBid {
    implicit val json: OFormat[AddToBid] = Json.format[AddToBid]
  }
}

case class UserState(
  user: UUID,
  ownedItems: List[Item],
  charity: Long,
  items: List[UserItem]
)

object UserState {
  implicit val userStateJsonFormat: OFormat[UserState] = Json.format[UserState]
}

case class UserItem(
  gameStatus: String,
  gameStarted: Option[Long],
  gameEnds: Option[Long],
  chanceOfWinning: Option[Double],
  moneySpent: Option[Int],
  percentageAchieved: Double,
  item: Item
)

object UserItem {
  implicit val userGameStateJsonFormat: OFormat[UserItem] = Json.format[UserItem]

  def fromUserData(user: UserData, game: Game): UserItem = {
    val bid = game.bids.get(user.id)
    val chances = game.winningChancesForUser(user.id)

    UserItem(game.status.stringMessage, game.startTime, game.endTime, chances, bid, game.percentageAchieved, game.item)
  }

  def fromItem(user: UserData, item: Item): UserItem =
    UserItem.fromUserData(user, Game(item))
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