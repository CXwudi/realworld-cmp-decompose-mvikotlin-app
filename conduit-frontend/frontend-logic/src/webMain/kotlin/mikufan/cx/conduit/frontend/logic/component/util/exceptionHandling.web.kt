package mikufan.cx.conduit.frontend.logic.component.util

import kotlin.coroutines.cancellation.CancellationException

actual inline fun <reified T : Throwable> rethrowIfShouldNotBeHandled(
  throwable: T,
  block: (T) -> Unit
) {
  when (throwable) {
    is CancellationException -> throw throwable
    else -> block(throwable) // JsError is an Error not an Exception
  }
}