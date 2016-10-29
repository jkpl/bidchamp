package model

object ExampleData {

  val player1 = User("Jaakko")
  val player2 = User("Olivier")
  val player3 = User("Michal")
  val player4 = User("Andrew")

  val game = Game(
    item = Item("MacBook Pro", 2000),
    bids = Map(player1 -> Bid(1000), player2 -> Bid(1500), player3 -> Bid(1500), player4 -> Bid(600)),
    status = NotStarted
  )
}
