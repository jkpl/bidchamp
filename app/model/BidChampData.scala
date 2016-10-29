package model

import java.util.UUID

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

  def evalCommand(command: Command): Result = command match {
    case Refresh =>
      justEvents("lollerz")

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

  def evalAuthCommand(user: User, command: AuthCommand): Result = command match {
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
          val updatedGame = game.upsertBid(user, bid.amount)
          Result(
            updateGame(updatedGame),
            List(s"User '${user.name}' bid ${bid.amount} for item ${game.item.toString}.")
          )
        case Some(game) =>
          justEvents(s"Can't bid on finished games.")
      }
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

  object Refresh extends Command
  case class AddUser(user: String) extends Command
  case class StartBid(userId: UUID, item: String, amount: Int) extends AuthCommand
  case class AddToBid(userId: UUID, game: Long, amount: Int) extends AuthCommand
}

case class UserData(
  user: User,
  spent: Int = 0
) {
  def id = user.uuid
}

case class Charity(
  contributed: Int
)