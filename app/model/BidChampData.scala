package model

case class BidChampData(
  items: Map[String, Item] = Map.empty,
  users: Map[String, UserData] = Map.empty,
  games: Map[Long, Game] = Map.empty,
  charity: Charity = Charity(0)
) {
  import BidChampData._

  val lastGameId = if (games.isEmpty) 0L else games.keySet.max

  def apply(command: Command): Result = command match {
    case bid: StartBid => ???
    case bid: AddToBid => ???
  }
}

object BidChampData {
  type Event = String
  type Result = (BidChampData, List[Event])

  sealed trait Command
  case class StartBid(player: String, item: String, amount: Int) extends Command
  case class AddToBid(player: String, game: Long, amount: Int) extends Command
}

case class UserData(
  user: User,
  spent: Int
)

case class Charity(
  contributed: Int
)