package mikufan.cx.conduit.frontend.app.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.github.oshai.kotlinlogging.KotlinLogging
import mikufan.cx.conduit.frontend.ui.setupAndStartMainUI

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log.info { "onCreate" }

    val dependencies = resolveAndroidAppDependencies(this)

    enableEdgeToEdge()
    setContent {
      setupAndStartMainUI(dependencies)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    log.info { "onDestroy" }
  }
}

private val log = KotlinLogging.logger { }
