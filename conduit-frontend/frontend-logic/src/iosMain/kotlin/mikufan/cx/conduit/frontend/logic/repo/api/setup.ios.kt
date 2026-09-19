package mikufan.cx.conduit.frontend.logic.repo.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

actual fun setupKtorClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(Darwin) {
  config(this)
}