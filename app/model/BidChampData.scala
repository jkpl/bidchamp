package model

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
    case Refresh =>
      justEvents("lollerz")

    case AddUser(username) =>
      if (users.contains(username))
        justEvents(s"User '$username' already exists.")
      else {
        (copy(users = users + (username -> UserData(User(username)))),
          List(s"User '$username' created"))
      }

    case authCmd: AuthCommand =>
      users.get(authCmd.user) match {
        case None => justEvents(s"No such user '${authCmd.user}' found.")
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
          (updateGame(game),
            List(s"Added game #${game.id} for item '${item.name}'."))
      }

    case bid: AddToBid =>
      games.get(bid.game) match {
        case None => justEvents(s"No game #${bid.game} found.")
        case Some(game) if game.isActive =>
          val updatedGame = game.upsertBid(user, bid.amount)
          (updateGame(updatedGame),
            List(s"User '${user.name}' bid ${bid.amount} for item ${game.item.toString}."))
        case Some(game) =>
          justEvents(s"Can't bid on finished games.")
      }
  }

  private def updateGame(game: Game) = copy(games = games + (game.id -> game))

  private def justEvents(events: Event*): Result = (this, events)
}

object BidChampData {
  type Event = String
  type Result = (BidChampData, Seq[Event])

  sealed trait Command
  sealed trait AuthCommand extends Command {
    def user: String
  }

  object Refresh extends Command
  case class AddUser(username: String) extends Command
  case class StartBid(user: String, item: String, amount: Int) extends AuthCommand
  case class AddToBid(user: String, game: Long, amount: Int) extends AuthCommand
}

case class UserData(
  user: User,
  spent: Int = 0
)

case class Charity(
  contributed: Int
)