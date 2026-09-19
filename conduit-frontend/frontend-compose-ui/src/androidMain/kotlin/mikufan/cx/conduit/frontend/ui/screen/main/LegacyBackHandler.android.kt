package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arkivanov.essenty.backhandler.BackDispatcher

@Composable
actual fun LegacyBackHandler(backDispatcher: BackDispatcher) {
  var isEnabled by remember { mutableStateOf(backDispatcher.isEnabled) }

  DisposableEffect(backDispatcher) {
    val listener: (Boolean) -> Unit = { enabled -> isEnabled = enabled }
    backDispatcher.addEnabledChangedListener(listener)
    onDispose {
      backDispatcher.removeEnabledChangedListener(listener)
    }
  }

  BackHandler(enabled = isEnabled) {
    backDispatcher.back()
  }
}
