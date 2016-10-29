package model

case class Game(item : Item, bids : Map[Player, Bid], status : Status){ self =>

  private def nbOfItemsInPlay() : Int = bids.values.map(_.amount).sum / item.price

  private def exceedAmount() : Int = bids.values.map(_.amount).sum % item.price

  private def updateStatus() : Game = nbOfItemsInPlay() match {
    case 0 => self.copy(status = NotStarted)
    case n if n >= 1 => self.copy(status = Running(n))
  }

  def addSinglePlayer(user : User, bid: Bid) : Game = self.copy(bids = self.bids + (user -> bid)).updateStatus()

  def createTeam(teamName : String, members : List[User]) : Game = {
    val notInTeam = bids.filterKeys {
      case u : User if members.contains(u) => false
      case _ => true
    }
    val teamBid = bids.collect {
      case (u@User(_), bid) if members.contains(u) => bid.amount
    }.sum
    self.copy(bids =notInTeam + (Team(teamName, members) -> Bid(teamBid)))
  }.updateStatus()

  def upsertBid(player : Player, additionalAmount : Int) : Game = {
    val oldBidMaybe : Option[(Player, Bid)] = bids.collectFirst {
      case (p, bid) if p == player => (player, bid)
      case (t : Team, bid) if player.isInstanceOf[User] && t.users.contains(player.asInstanceOf[User]) => (t, bid)
    }
    oldBidMaybe.fold(
      self.copy(bids = self.bids + (player -> Bid(additionalAmount)))
    ){case (p, b) => self.copy(bids = self.bids + (p -> Bid(b.amount + additionalAmount)))}
  }



}

object Game {
  def newInstance(item : Item) = Game(item, Map(), NotStarted)
}

sealed trait Player
case class User(name : String) extends Player
case class Team(teamName : String, users : List[User]) extends Player

case class Bid(amount : Int)
case class Item(name : String, price : Int)

sealed trait Status
case object NotStarted extends Status
case class Running(nbItems : Int) extends Status
case class Finished(winner : List[Player]) extends Status




