package services

import model.{Bid, Game, User}

import scala.util.Random

object DrawWinners {

  def apply(game: Game): List[User] = drawMultiple(game)

  private def drawMultiple(game: Game) = {
    def iter(itemsLeft: Int, bids: Map[User, Bid], winners: List[User]): List[User] =
      if (itemsLeft == 0) winners
      else {
        val winner = drawOne(bids)
        val nextBids = bids - winner
        iter(itemsLeft - 1, nextBids, winner :: winners)
      }

    iter(game.itemsToWin, game.bids, Nil)
  }

  private def drawOne(bids: Iterable[(User, Bid)]): User = {
    val max = bids.map(_._2.amount).sum
    val winningNumber = Random.nextInt(max)

    val players = for {
      (player, bid) <- bids
      _ <- 1 to bid.amount
    } yield player

    players.toVector(winningNumber)
  }
}
