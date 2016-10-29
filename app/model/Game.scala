package model

case class Game(item : Item, bids : Map[Player, Bid], Status : Status)

sealed trait Player
case class User(name : String) extends Player
case class Team(users : List[User]) extends Player

case class Bid(amount : Float)
case class Item(name : String, price : Float)

sealed trait Status
case object NotStarted extends Status
case object Running extends Status
case class Finished(winner : List[Player]) extends Status





