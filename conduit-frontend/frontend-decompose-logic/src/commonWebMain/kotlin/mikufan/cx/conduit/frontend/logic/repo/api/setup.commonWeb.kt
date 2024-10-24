package mikufan.cx.conduit.frontend.logic.repo.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory

expect val Js: HttpClientEngineFactory<*>

actual fun setupKtorClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(Js) {
  config(this)
}