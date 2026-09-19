package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.runtime.Composable
import com.arkivanov.essenty.backhandler.BackDispatcher

@Composable
actual fun LegacyBackHandler(backDispatcher: BackDispatcher) {
  // Web Wasm has no hardware back button
}
