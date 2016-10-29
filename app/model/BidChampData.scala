package model

import play.api.libs.json._

case class BidChampData(
  items: Map[String, Item] = Map.empty,
  users: Map[String, UserData] = Map.empty,
  games: Map[Long, Game] = Map.empty,
  charity: Charity = Charity(0)
) {
  import BidChampData._

  private def nextGameId: Long =
    if (games.isEmpty) 0L
    else games.keySet.max + 1

  def evalCommand(command: Command): Result = command match {
    case AddUser(username) =>
      if (users.contains(username))
        justEvents(s"User '$username' already exists.")
      else {
        (copy(users = users + (username -> UserData(User(username)))),
          List(s"User '$username' created"))
      }

  }

  def evalAuthCommand(user: User, command: AuthCommand): Result = command match {
    case bid: StartBid =>
      items.get(bid.item) match {
        case None =>
          justEvents(s"Item '${bid.item}' doesn't exist.")
        case Some(item) =>
          val game = Game.newInstance(nextGameId, item).upsertBid(user, bid.amount)
          (copy(games = games + (game.id -> game)),
            List(s"Added game #'${game.id}' for item '${item.name}'."))
      }

    case bid: AddToBid => ???
  }

  private def justEvents(events: Event*): Result = (this, events)
}

object BidChampData {
  type Event = String
  type Result = (BidChampData, Seq[Event])

  sealed trait Command
  object Command {
    implicit val reads : Reads[Command] = new Reads[Command]{
      override def reads(json: JsValue): JsResult[Command] = json \ "command" match {
        case JsString("addUser") => (json \ "payload").validate[AddUser]
        case _ => JsError.apply("Command not recognised")
      }
    }
  }

  case class AddUser(username: String) extends Command
  object AddUser {
    implicit val json: OFormat[AddUser] = Json.format[AddUser]
  }

  sealed trait AuthCommand {
    def user: String
  }
  object AuthCommand {
    implicit val reads : Reads[AuthCommand] = new Reads[AuthCommand]{
      override def reads(json: JsValue): JsResult[AuthCommand] = json \ "command" match {
        case JsString("startBid") => (json \ "payload").validate[StartBid]
        case JsString("addToBid") => (json \ "payload").validate[AddToBid]
        case _ => JsError.apply("Command not recognised")
      }
    }
  }
  case class StartBid(user: String, item: String, amount: Int) extends AuthCommand
  object StartBid {
    implicit val json: OFormat[StartBid] = Json.format[StartBid]
  }
  case class AddToBid(user: String, game: Long, amount: Int) extends AuthCommand
  object AddToBid {
    implicit val json: OFormat[AddToBid] = Json.format[AddToBid]
  }
}

case class UserData(
  user: User,
  spent: Int = 0
)

case class Charity(
  contributed: Int
)