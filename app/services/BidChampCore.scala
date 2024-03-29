package services

import javax.inject._

import actors.BidChampActor
import akka.actor.{ActorRef, ActorSystem}


trait BidChampCore {
  def gameActor : ActorRef
}

@Singleton
class AtomicBidChampCore @Inject()(actorSystem : ActorSystem) extends BidChampCore {
  override lazy val gameActor: ActorRef = actorSystem.actorOf(BidChampActor.props)
}
