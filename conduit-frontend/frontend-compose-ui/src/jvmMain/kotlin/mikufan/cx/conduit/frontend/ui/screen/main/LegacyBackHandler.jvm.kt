package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.runtime.Composable
import com.arkivanov.essenty.backhandler.BackDispatcher

@Composable
actual fun LegacyBackHandler(backDispatcher: BackDispatcher) {
  // Desktop has no system back gesture/button
}
