package mikufan.cx.conduit.frontend.app.desktop

import mikufan.cx.conduit.frontend.logic.AppDependencies
import mikufan.cx.conduit.frontend.logic.allModules
import mikufan.cx.conduit.frontend.logic.createAppDependencies
import org.koin.dsl.koinApplication

/**
 * Initializes Koin DI container for Desktop and returns plain [AppDependencies].
 * Bridges Koin shutdown to the application window lifecycle.
 */
fun initDesktopAppDependencies(): AppDependencies {
  val koinApp = koinApplication {
    modules(allModules)
  }
  return createAppDependencies(
    koin = koinApp.koin,
    onShutdown = { koinApp.close() },
  )
}
