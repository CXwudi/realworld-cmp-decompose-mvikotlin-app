package mikufan.cx.conduit.frontend.app.android

import android.app.Application
import android.content.Context
import mikufan.cx.conduit.frontend.logic.AppDependencies
import mikufan.cx.conduit.frontend.logic.allModules
import mikufan.cx.conduit.frontend.logic.createAppDependencies
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Initializes Koin for Android once during Application startup.
 */
fun initAndroidKoin(application: Application) {
  if (GlobalContext.getOrNull() == null) {
    startKoin {
      androidContext(application)
      modules(allModules)
    }
  }
}

/**
 * Resolves [AppDependencies] for Android.
 * Android DI lives in the Application scope and survives Activity recreation (rotation).
 * The shutdown callback is a no-op so that Activity destruction does not close the container.
 */
fun resolveAndroidAppDependencies(context: Context): AppDependencies {
  val koin = GlobalContext.get()
  return createAppDependencies(
    koin = koin,
    onShutdown = { /* Do not close DI on Activity recreation/rotation */ },
  )
}
