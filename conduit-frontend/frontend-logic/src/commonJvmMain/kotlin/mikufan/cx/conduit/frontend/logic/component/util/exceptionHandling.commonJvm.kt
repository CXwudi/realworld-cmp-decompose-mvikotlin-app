package mikufan.cx.conduit.frontend.logic.component.util

import kotlin.coroutines.cancellation.CancellationException

actual inline fun <reified T : Throwable> rethrowIfShouldNotBeHandled(
  throwable: T,
  block: (T) -> Unit
) {
  when (throwable) {
    is InterruptedException -> throw throwable
    is CancellationException -> throw throwable
    is Exception -> block(throwable)
    else -> throw throwable
  }
}