package mikufan.cx.conduit.frontend.logic.poc

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import kotlin.test.Ignore
import kotlin.test.Test

class HttpPoc {

  @Test
  @Ignore
  fun tryKtorUrlBuilder() {
    val pathBuilder = URLBuilder("users")
    log.info { pathBuilder.build() }
    val anotherUrlBuilder = URLBuilder("https://some.domain:8080/api")
    pathBuilder.apply {
      protocolOrNull = anotherUrlBuilder.protocolOrNull
      host = anotherUrlBuilder.host
      port = anotherUrlBuilder.port
      encodedPathSegments = anotherUrlBuilder.encodedPathSegments + encodedPathSegments
    }
    log.info { pathBuilder.build() }

  }

  @Test
  @Ignore
  fun tryAppendUrl2() {
    val pathBuilder = URLBuilder("users?userId=1")
    log.info { pathBuilder.build() }
    val anotherUrlBuilder = URLBuilder("https://user:pass@some.domain:8080/api")
    anotherUrlBuilder.apply {
      appendEncodedPathSegments(pathBuilder.encodedPathSegments)
      parameters.appendAll(pathBuilder.parameters.build())
    }
    log.info { anotherUrlBuilder.build() }
  }
}

private val log = KotlinLogging.logger { }