package services

import javax.inject._

import common.AtomicRef

trait BidChampCore {
  def nextState(): Int
}

@Singleton
class AtomicBidChampCore extends BidChampCore {
  private val ref = new AtomicRef[Int](0)

  override def nextState(): Int = ref.update(_ + 1)
}
