package mikufan.cx.conduit.frontend.app.android

import io.github.oshai.kotlinlogging.KotlinLogging
import mikufan.cx.conduit.frontend.logic.allModules
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.koinConfiguration

@KoinExperimentalAPI
class MainApplication : android.app.Application(), KoinStartup {

  override fun onCreate() {
    super.onCreate()
    log.info { "onCreate" }
  }

  override fun onKoinStartup(): KoinConfiguration = koinConfiguration {
    androidContext(this@MainApplication)
    modules(allModules)
  }

}

private val log = KotlinLogging.logger { }