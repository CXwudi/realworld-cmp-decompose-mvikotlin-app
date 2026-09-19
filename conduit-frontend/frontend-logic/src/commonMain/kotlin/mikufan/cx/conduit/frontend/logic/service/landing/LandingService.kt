package mikufan.cx.conduit.frontend.logic.service.landing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import mikufan.cx.conduit.common.ArticlesRsp
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore


interface LandingService {
  /**
   * Check accessibility of the given [url].
   *
   * @throws Exception if in any case the [url] is not accessible.
   */
  suspend fun checkAccessibilityAndSetUrl(url: String)
}

class DefaultLandingService(
  private val httpClient: HttpClient,
  private val userConfigKStore: UserConfigKStore,
) : LandingService {

  companion object {
    private val ALLOWED_PROTOCOLS = setOf("http", "https")
  }

  override suspend fun checkAccessibilityAndSetUrl(url: String) {
    val normalizedUrl = when {
      // Extract and validate existing protocol
      url.contains("://") -> {
        val protocol = url.substringBefore("://")
        if (protocol.lowercase() !in ALLOWED_PROTOCOLS) {
          error("Invalid protocol: $protocol. Only HTTP/HTTPS allowed")
        }
        url
      }
      // Add default http protocol if missing
      else -> "http://$url"
    }.removeSuffix("/")

    val builtUrl = URLBuilder(normalizedUrl)
    // send a request to a public api to check if the url is accessible
    val httpResponse = httpClient.get {
      url {
        takeFrom(builtUrl).appendEncodedPathSegments("articles")
      }
      timeout {
        requestTimeoutMillis = 10000
      }
    }

    if (!httpResponse.status.isSuccess()) {
      error("Failed with status ${httpResponse.status}: ${httpResponse.bodyAsText()}")
    }

    // make sure the response is what we are looking for
    val articlesRsp = httpResponse.body<ArticlesRsp>()
    val isEmptyResponse = articlesRsp.articlesCount == 0 && articlesRsp.articles.isEmpty()
    val isValidResponse = articlesRsp.articlesCount > 0 &&
        articlesRsp.articles.isNotEmpty() && articlesRsp.articles.first().title.isNotEmpty()

    if (isEmptyResponse || isValidResponse) {
      userConfigKStore.setUrl(builtUrl.buildString())
    } else {
      error("Invalid response")
    }
  }

}