package mikufan.cx.conduit.frontend.app.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.startup.AppInitializer
import com.arkivanov.decompose.defaultComponentContext
import io.github.oshai.kotlinlogging.KotlinLogging
import mikufan.cx.conduit.frontend.logic.component.RootNavComponentFactory
import mikufan.cx.conduit.frontend.ui.setupAndStartMainUI
import org.koin.androix.startup.KoinInitializer
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.GlobalContext
import org.koin.mp.KoinPlatformTools


class MainActivity : AppCompatActivity() {
  @OptIn(KoinExperimentalAPI::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log.info { "onCreate" }

    val defaultComponentContext = defaultComponentContext()

    // this will retrieve the Koin instance cached in app initializer
    // see comments in https://stackoverflow.com/a/62395631/8529009
    AppInitializer.getInstance(application).initializeComponent(KoinInitializer::class.java)
    val koinApp = GlobalContext.getKoinApplicationOrNull() ?: error("Koin app is not initialized")

    val rootComponent = koinApp.koin.get<RootNavComponentFactory>().create(defaultComponentContext)

    enableEdgeToEdge()
    setContent {
      setupAndStartMainUI(koinApp, rootComponent)
    }
  }


  override fun onDestroy() {
    super.onDestroy()
    log.info { "onDestroy" }
  }
}

private val log = KotlinLogging.logger { }
