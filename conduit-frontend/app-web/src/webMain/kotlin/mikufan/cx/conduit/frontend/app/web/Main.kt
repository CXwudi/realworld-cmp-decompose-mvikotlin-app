package mikufan.cx.conduit.frontend.app.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import mikufan.cx.conduit.frontend.app.web.setup.afterComposeSetup
import mikufan.cx.conduit.frontend.app.web.setup.attachToDocument
import mikufan.cx.conduit.frontend.app.web.setup.initWebAppDependencies
import mikufan.cx.conduit.frontend.app.web.setup.launchApp
import mikufan.cx.conduit.frontend.ui.setupAndStartMainUI

@OptIn(ExperimentalComposeUiApi::class, DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
  val lifecycle = LifecycleRegistry()
  lifecycle.attachToDocument()

  lifecycle.launchApp {
    // initialize within a global coroutine, preserving asynchronous web bootstrap
    val dependencies = initWebAppDependencies()
    lifecycle.doOnDestroy { dependencies.onShutdown() }

    log.info { "Starting" }

    ComposeViewport { // using the default empty <body/> tag if not specified
      afterComposeSetup(newTitle = "Conduit Web")
      setupAndStartMainUI(dependencies)
    }
  }
}

private val log = KotlinLogging.logger { }
