package services

import java.util.UUID

import model.Game

import scala.util.Random

object DrawWinners {

  def apply(game: Game): List[UUID] = drawMultiple(game)

  private def drawMultiple(game: Game) = {
    def iter(itemsLeft: Int, bids: Map[UUID, Int], winners: List[UUID]): List[UUID] =
      if (itemsLeft == 0) winners
      else {
        val winner = drawOne(bids)
        val nextBids = bids - winner
        iter(itemsLeft - 1, nextBids, winner :: winners)
      }

    iter(game.itemsToWin, game.bids, Nil)
  }

  private def drawOne(bids: Map[UUID, Int]): UUID = {
    val max = bids.values.sum
    val winningNumber = Random.nextInt(max)

    val players = for {
      (player, bid) <- bids
      _ <- 1 to bid
    } yield player

    players.toVector(winningNumber)
  }
}
