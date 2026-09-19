package mikufan.cx.conduit.frontend.app.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.oshai.kotlinlogging.KotlinLogging
import mikufan.cx.conduit.frontend.ui.setupAndStartMainUI

fun main(args: Array<String>) {
  val dependencies = initDesktopAppDependencies()

  log.info { "Starting" }

  application {
    val windowState = rememberWindowState()

    Window(
      onCloseRequest = {
        dependencies.onShutdown()
        exitApplication()
      },
      title = "Conduit Desktop",
      state = windowState,
    ) {
      setupAndStartMainUI(dependencies)
    }
  }
}

private val log = KotlinLogging.logger { }
