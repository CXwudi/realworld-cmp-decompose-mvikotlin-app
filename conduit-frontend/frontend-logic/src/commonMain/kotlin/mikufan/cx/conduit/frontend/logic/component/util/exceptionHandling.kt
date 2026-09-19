package mikufan.cx.conduit.frontend.logic.component.util

/**
 * This function is created to properly handle exceptions based on different platform.
 *
 * Normally the best practice is to handle [Exception] but leave the [Error] and any multithreaded or coroutine related exceptions.
 * However, due to Js and Wasm platform wrapping all Js exceptions into [Error] object,
 * we need to handle [Error] for those platforms.
 */
expect inline fun <reified T : Throwable> rethrowIfShouldNotBeHandled(throwable: T, block: (T) -> Unit)

