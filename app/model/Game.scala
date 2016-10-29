package model

import services.DrawWinners
import java.util.UUID

import play.api.libs.json._

case class Game(item : Item, bids : Map[UUID, Int] = Map.empty, status : Status = NotStarted) { self =>
  val id = item.name
  val moneyPooled: Int = bids.values.sum
  val itemsToWin: Int = moneyPooled / item.price
  val percentageAchieved: Double = moneyPooled.toDouble / item.price
  val exceedAmount: Int = bids.values.sum % item.price

  def winningChancesForUser(userId: UUID): Option[Double] =
    bids.get(userId).map { bid =>
      val divisor = itemsToWin match {
        case 0 => item.price // not enough money pooled yet
        case 1 => moneyPooled // enough money pooled for one item
        case _ => item.price * 2 - 1 // minimum chances
      }
      bid.toDouble / divisor
    }

  def updateStatus() : Game = status match {
    case NotStarted if itemsToWin > 1 =>
      self.copy(status = Running(System.currentTimeMillis()))
    case s: Running if s.endTime < System.currentTimeMillis() =>
      self.copy(status = Finished(s.startTime, s.endTime, DrawWinners(self)))
    case _ =>
      self
  }

  def upsertBid(user : UUID, additionalAmount : Int) : Game = {
    val updatedBid = bids.get(user).map(_ + additionalAmount).getOrElse(additionalAmount)
    self.copy(bids = self.bids + (user -> updatedBid))
  }

  def isPassive: Boolean = status match {
    case _: Finished => true
    case _ => false
  }

  def isActive: Boolean = !isPassive

  def startTime: Option[Long] = status match {
    case Running(startTime, _) => Some(startTime)
    case Finished(startTime, _, _) => Some(startTime)
    case _ => None
  }

  def endTime: Option[Long] = status match {
    case Running(_, endTime) => Some(endTime)
    case Finished(_, endTime, _) => Some(endTime)
    case _ => None
  }
}

case class Item(name : String, price : Int)

object Item {
  implicit val itemJsonFormat: OFormat[Item] = Json.format[Item]
}

sealed trait Status {
  def stringMessage: String
}

case object NotStarted extends Status {
  override def stringMessage: String = "Waiting for bids..."
}

case class Running(startTime: Long, endTime: Long) extends Status {
  override def stringMessage: String = "Game is about to close!"
}

object Running {
  def apply(startTime: Long): Running = Running(startTime, startTime + (10 * 60 * 1000).toLong) // +10 minutes
}

case class Finished(startTime: Long, endTime: Long, winners : List[UUID]) extends Status {
  override def stringMessage: String = "Game finished!"
}
