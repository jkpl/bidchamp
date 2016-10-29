package model

import services.DrawWinners

import java.util.UUID

case class Game(id: Long, item : Item, bids : Map[User, Bid], status : Status) { self =>
  val moneyPooled: Int = bids.map(_._2.amount).sum
  val itemsToWin: Int = moneyPooled / item.price
  val percentageAchieved: Double = moneyPooled.toDouble / item.price
  val exceedAmount: Int = bids.values.map(_.amount).sum % item.price

  def winningChances: Map[User, Double] =
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

  def upsertBid(user : User, additionalAmount : Int) : Game = {
    val updatedBid = bids.get(user).map(_.increase(additionalAmount)).getOrElse(Bid(additionalAmount))
    self.copy(bids = self.bids + (user -> updatedBid))
  }

  def isPassive: Boolean = status match {
    case _: Finished => true
    case _ => false
  }

  def isActive: Boolean = !isPassive
}

object Game {
  val gameTimeout = (10 * 60 * 1000).toLong // 10 minutes

  def newInstance(id: Long, item : Item) = Game(id, item, Map(), NotStarted)
}

case class User(name : String, uuid: UUID = UUID.randomUUID())

case class Bid(amount : Int) {
  def increase(a: Int): Bid = Bid(amount + a)
}
case class Item(name : String, price : Int)

sealed trait Status
case object NotStarted extends Status
case class Running(startTime: Long) extends Status
case class Finished(winner : List[User]) extends Status
