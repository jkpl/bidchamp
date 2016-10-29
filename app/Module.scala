import com.google.inject.AbstractModule
import services.{AtomicBidChampCore, BidChampCore}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[BidChampCore]).to(classOf[AtomicBidChampCore])
  }
}
