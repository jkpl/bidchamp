package services

import javax.inject._

import actors.GameActor
import akka.actor.{ActorRef, ActorSystem}


trait BidChampCore {
  def gameActor : ActorRef
}

@Singleton
class AtomicBidChampCore @Inject()(actorSystem : ActorSystem )extends BidChampCore {
  override def gameActor: ActorRef = actorSystem.actorOf(GameActor.props())
}
