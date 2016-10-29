import com.google.inject.AbstractModule
import services.{AtomicBidChampCore, BidChampCore, MemoryUserStore, UserStore}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[BidChampCore]).to(classOf[AtomicBidChampCore])
    bind(classOf[UserStore]).to(classOf[MemoryUserStore])
  }
}
