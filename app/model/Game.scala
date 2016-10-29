package model

case class Game(item : Item, bids : Map[Player, Bid], status : Status) {
  val moneyPooled: Int = bids.map(_._2.amount).sum
  val itemsToWin: Int = moneyPooled / item.price

  def winningChances: Map[Player, Double] =
    bids.mapValues { bid =>
      val divisor = itemsToWin match {
        case 0 => item.price // not enough money pooled yet
        case 1 => moneyPooled // enough money pooled for one item
        case _ => item.price * 2 - 1 // minimum chances
      }
      bid.amount.toDouble / divisor
    }

  val percentageAchieved: Double = moneyPooled.toDouble / item.price
}

sealed trait Player
case class User(name : String) extends Player
case class Team(name : String, users : List[User]) extends Player

case class Bid(amount : Int)
case class Item(name : String, price : Int)

sealed trait Status
case object NotStarted extends Status
case object Running extends Status
case class Finished(winner : List[Player]) extends Status
