package mikufan.cx.conduit.frontend.app.android

import android.app.Application
import io.github.oshai.kotlinlogging.KotlinLogging

class MainApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    log.info { "onCreate" }
    initAndroidKoin(this)
  }
}

private val log = KotlinLogging.logger { }
