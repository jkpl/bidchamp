package services

import model.{Bid, Game, Player}

import scala.util.Random

object DrawWinners {

  def apply(game: Game): List[Player] = drawMultiple(game)

  private def drawMultiple(game: Game) = {
    val max = game.bids.map(_._2.amount).sum
    val itemsToWin = max / game.item.price

    def iter(itemsLeft: Int, bids: Map[Player, Bid], winners: List[Player]): List[Player] =
      if (itemsLeft == 0) winners
      else {
        val winner = drawOne(bids)
        val nextBids = bids - winner
        iter(itemsLeft - 1, nextBids, winner :: winners)
      }

    iter(itemsToWin, game.bids, Nil)
  }

  private def drawOne(bids: Iterable[(Player, Bid)]): Player = {
    val max = bids.map(_._2.amount).sum
    val winningNumber = Random.nextInt(max)

    val players = for {
      (player, bid) <- bids
      _ <- 1 to bid.amount
    } yield player

    players.toVector(winningNumber)
  }
}
