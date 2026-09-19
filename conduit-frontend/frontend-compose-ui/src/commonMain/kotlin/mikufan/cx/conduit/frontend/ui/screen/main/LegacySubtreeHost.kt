package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mikufan.cx.conduit.frontend.logic.component.legacy.LegacyChildAdapterViewModel

/**
 * Host Composable bridging a [LegacyChildAdapterViewModel] into the Compose hierarchy.
 *
 * Responsibilities:
 * - Safely forwards native lifecycle transitions to Essenty's [com.arkivanov.essenty.lifecycle.LifecycleRegistry].
 *   Disposal during Android configuration changes (rotation) does not destroy the retained
 *   legacy tree; destruction happens only when the native ViewModel clears.
 * - Guarded against replay of initial events and clear-before-disposal ordering: if the adapter
 *   registry is already DESTROYED (cleared), subsequent events and disposal are no-ops.
 * - Bridges Essenty's back dispatcher to the platform system back handler via [LegacyBackHandler].
 * - Renders the wrapped legacy component content.
 */
@Composable
fun <T : Any> LegacySubtreeHost(
  adapter: LegacyChildAdapterViewModel<T>,
  modifier: Modifier = Modifier,
  content: @Composable (T) -> Unit,
) {
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, adapter) {
    val observer = LifecycleEventObserver { _, event ->
      adapter.syncLifecycleEvent(event)
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      adapter.onHostDisposed()
    }
  }

  LegacyBackHandler(adapter.backDispatcher)

  Box(modifier = modifier) {
    content(adapter.component)
  }
}
