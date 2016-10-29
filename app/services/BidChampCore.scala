package services

import java.util.concurrent.atomic.AtomicInteger
import javax.inject._

trait BidChampCore {
  def nextCount(): Int
}

@Singleton
class AtomicBidChampCore extends BidChampCore {
  private val atomicCounter = new AtomicInteger()
  override def nextCount(): Int = atomicCounter.getAndIncrement()
}
