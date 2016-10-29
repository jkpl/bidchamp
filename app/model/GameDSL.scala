package model

import cats.data.State

trait GameDSL {


  def addSingle(player : Player, bid : Bid) = State[Game, Unit]{ state =>
    state.copy()

  }


}
