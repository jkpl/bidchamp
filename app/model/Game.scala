package model

case class Game(item : Item, bids : Map[Player, Bid], status : Status)

sealed trait Player
case class User(name : String) extends Player
case class Team(name : String, users : List[User]) extends Player

case class Bid(amount : Int)
case class Item(name : String, price : Int)

sealed trait Status
case object NotStarted extends Status
case object Running extends Status
case class Finished(winner : List[Player]) extends Status
