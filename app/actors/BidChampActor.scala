package actors

import java.util.UUID

import akka.actor._
import model.BidChampData
import model.BidChampData.InternalCommand

object BidChampActor {
  case class Subscribe(uuid: UUID)
  case object Unsubscribe

  def props: Props = Props(new BidChampActor)
}

/**
  * Holds the state of the application
  */
class BidChampActor extends Actor {

  import BidChampActor._

  override def receive: Receive = ready(Map.empty, BidChampData())

  private def ready(clients: Map[UUID, ActorRef], state: BidChampData): Receive = {
    case Subscribe(uuid) => context.become(ready(clients + (uuid -> sender()), state))
    case Unsubscribe => context.become(ready(clients.filter(_._2 == sender()), state))
    case cmd: InternalCommand =>
      val result = state.eval(cmd)
      context.become(ready(clients, result.state))

      for {
        event <- result.events
        if event.targets.isEmpty
      } sender() ! event.content

      for {
        event <- result.events
        target <- event.targets
        client <- clients.get(target)
      } client ! event.content
  }
}
