package common

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

final class AtomicRef[V](initial: V) {
  private val ref = new AtomicReference[V](initial)

  def update(f: V => V): V =
    ref.updateAndGet(new UnaryOperator[V] {
      override def apply(t: V): V = f(t)
    })

  def get: V = ref.get()
}
