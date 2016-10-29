package model

import services.DrawWinners

import java.util.UUID

case class Game(item : Item, bids : Map[Player, Bid], status : Status) { self =>
  val moneyPooled: Int = bids.map(_._2.amount).sum
  val itemsToWin: Int = moneyPooled / item.price
  val percentageAchieved: Double = moneyPooled.toDouble / item.price
  val exceedAmount: Int = bids.values.map(_.amount).sum % item.price

  def winningChances: Map[Player, Double] =
    bids.mapValues { bid =>
      val divisor = itemsToWin match {
        case 0 => item.price // not enough money pooled yet
        case 1 => moneyPooled // enough money pooled for one item
        case _ => item.price * 2 - 1 // minimum chances
      }
      bid.amount.toDouble / divisor
    }

  def updateStatus() : Game = status match {
    case NotStarted if itemsToWin > 1 =>
      self.copy(status = Running(System.currentTimeMillis()))
    case Running(startTime) if startTime + Game.gameTimeout > System.currentTimeMillis() =>
      self.copy(status = Finished(DrawWinners(self)))
    case _ =>
      self
  }

  def addSinglePlayer(user : User, bid: Bid) : Game =
    self.copy(bids = self.bids + (user -> bid)).updateStatus()

  def createTeam(teamName : String, members : List[User]) : Game = {
    val notInTeam = bids.filterKeys {
      case u : User if members.contains(u) => false
      case _ => true
    }
    val teamBid = bids.collect {
      case (u@User(_,_), bid) if members.contains(u) => bid.amount
    }.sum

    self.copy(bids = notInTeam + (Team(teamName, members) -> Bid(teamBid))).updateStatus()
  }

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
  val gameTimeout = (10 * 60 * 1000).toLong // 10 minutes

  def newInstance(item : Item) = Game(item, Map(), NotStarted)
}

sealed trait Player {
  def name: String
}
case class User(name : String, uuid: UUID = UUID.randomUUID()) extends Player
case class Team(name : String, users : List[User]) extends Player

case class Bid(amount : Int)
case class Item(name : String, price : Int)

sealed trait Status
case object NotStarted extends Status
case class Running(startTime: Long) extends Status
case class Finished(winner : List[Player]) extends Status
