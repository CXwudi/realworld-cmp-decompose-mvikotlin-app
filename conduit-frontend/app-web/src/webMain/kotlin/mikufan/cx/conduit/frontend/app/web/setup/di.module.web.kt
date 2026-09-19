package mikufan.cx.conduit.frontend.app.web.setup

import mikufan.cx.conduit.frontend.logic.AppDependencies
import mikufan.cx.conduit.frontend.logic.allModules
import mikufan.cx.conduit.frontend.logic.createAppDependencies
import org.koin.dsl.koinApplication

/**
 * Initializes Koin DI container for Web with coroutine support and returns plain [AppDependencies].
 */
suspend fun initWebAppDependencies(): AppDependencies {
  val koinApp = koinApplication {
    modules(allModules)
  }
  return createAppDependencies(
    koin = koinApp.koin,
    onShutdown = { koinApp.close() },
  )
}
