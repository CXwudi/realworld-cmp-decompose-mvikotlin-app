package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.runtime.Composable
import com.arkivanov.essenty.backhandler.BackDispatcher

/**
 * iOS actual for [LegacyBackHandler].
 * System back button is not present on iOS.
 */
@Composable
actual fun LegacyBackHandler(backDispatcher: BackDispatcher) {
  // iOS has no default system back button handled here
}
