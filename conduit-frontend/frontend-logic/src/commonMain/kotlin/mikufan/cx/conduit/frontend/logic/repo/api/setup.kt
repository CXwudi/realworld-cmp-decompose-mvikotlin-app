package mikufan.cx.conduit.frontend.logic.repo.api

import de.jensklingenberg.ktorfit.Ktorfit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import mikufan.cx.conduit.frontend.logic.repo.api.util.ConduitResponseConverterFactory
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState

fun defaultJson(): Json = Json {
  ignoreUnknownKeys = true
  explicitNulls = false // used for the update user request when password is null -> don't update password
}

fun createDefaultHttpClient(userConfigKStore: UserConfigKStore, json: Json): HttpClient =
  setupKtorClient {
    install(ContentNegotiation) {
      json(json)
    }

    install(createBaseUrlAndTokenProvidingPlugin(userConfigKStore))
//    Logging {
//      logger = object : io.ktor.client.plugins.logging.Logger {
//        override fun log(message: String) {
//          log.debug { message }
//        }
//      }
//      level = LogLevel.HEADERS
//    }
  }

expect fun setupKtorClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

fun createKtorfit(
  httpClient: HttpClient,
  conduitResponseConverterFactory: ConduitResponseConverterFactory
): Ktorfit =
  Ktorfit.Builder()
    .httpClient(httpClient)
    .converterFactories(conduitResponseConverterFactory)
    .build()

/**
 * taken the idea from https://bbasoglu.medium.com/part-1-how-to-change-base-url-on-runtime-in-an-android-project-1d7607bbfa48
 *
 * Add a plugin to the HttpClient that intercepts change the base URL and token from the user config kstore.
 */
fun createBaseUrlAndTokenProvidingPlugin(userConfigKStore: UserConfigKStore) =
  createClientPlugin("BaseUrlAndTokenProvidingPlugin") {
    onRequest { requestBuilder, _ ->
      val userConfigState = userConfigKStore.userConfigFlow.first()
      val (url, token) = when (userConfigState) {
        is UserConfigState.Landing -> null to null
        is UserConfigState.OnUrl -> userConfigState.url to null
        is UserConfigState.OnLogin -> userConfigState.url to userConfigState.userInfo.token
      }

      url?.let { baseUrl ->
        requestBuilder.url { reqUrlBuilder ->
          val newUrlBuilder = URLBuilder(baseUrl).apply {
            appendEncodedPathSegments(reqUrlBuilder.encodedPathSegments)
            parameters.appendAll(reqUrlBuilder.parameters.build())
          }
          reqUrlBuilder.takeFrom(newUrlBuilder)
        }
      }

      token?.let {
        requestBuilder.headers["Authorization"] = "Token $it"
      }
    }
  }

private val log = KotlinLogging.logger { }