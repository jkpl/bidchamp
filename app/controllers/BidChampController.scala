package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import services.BidChampCore

@Singleton
class BidChampController @Inject() (bidChamp: BidChampCore) extends Controller {

  def state = Action { Ok(bidChamp.nextState().toString) }
}
