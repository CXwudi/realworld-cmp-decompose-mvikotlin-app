package mikufan.cx.conduit.frontend.app.web.setup

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import web.events.addHandler
import web.window.pageHideEvent
import web.window.unloadEvent
import web.window.window

private val log = KotlinLogging.logger { }

/**
 * Launches the web application within a scoped coroutine, ensuring asynchronous
 * startup and orderly DI/coroutine teardown upon page unload.
 *
 * Lifecycle management is natively handled by ComposeViewport (mapping window
 * focus/blur to ON_RESUME/ON_PAUSE, and document visibilitychange to ON_START/ON_STOP).
 */
fun launchApp(
  onShutdown: () -> Unit = {},
  appLaunch: suspend CoroutineScope.() -> Unit,
) {
  val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  appScope.launch {
    appLaunch()
  }

  var cleanedUp = false
  fun cleanup() {
    if (!cleanedUp) {
      cleanedUp = true
      log.info { "Shutting down web application" }
      onShutdown()
      appScope.cancel("Web page unloaded")
    }
  }

  // Handle page navigation / unload without destroying on BFCache preservation
  window.pageHideEvent.addHandler { event ->
    if (!event.persisted) {
      cleanup()
    }
  }

  // Fallback for browsers or situations where pagehide is bypassed
  window.unloadEvent.addHandler {
    cleanup()
  }
}
