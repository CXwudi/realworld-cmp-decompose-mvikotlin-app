package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mikufan.cx.conduit.frontend.logic.component.legacy.LegacyMainAdapterViewModel

/**
 * Host Composable bridging [LegacyMainAdapterViewModel] into the Compose hierarchy.
 *
 * Responsibilities:
 * - Safely forwards native lifecycle transitions to Essenty's [com.arkivanov.essenty.lifecycle.LifecycleRegistry].
 *   Disposal during Android configuration changes (rotation) must NOT destroy the retained
 *   legacy tree; destruction happens only when the native ViewModel clears.
 * - Guarded against replay of initial events and clear-before-disposal ordering: if the adapter
 *   registry is already DESTROYED (cleared), subsequent events and disposal are no-ops.
 * - Bridges Essenty's back dispatcher to the platform system back handler via [LegacyBackHandler].
 * - Renders the existing [MainNavPage].
 */
@Composable
fun LegacyMainPage(
  viewModel: LegacyMainAdapterViewModel,
  modifier: Modifier = Modifier,
) {
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, viewModel) {
    val observer = LifecycleEventObserver { _, event ->
      viewModel.syncLifecycleEvent(event)
    }

    // Adding the observer automatically dispatches initial events (ON_CREATE -> ON_START -> ON_RESUME)
    // up to the lifecycleOwner's current state without requiring manual synchronization.
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      viewModel.onHostDisposed()
    }
  }

  LegacyBackHandler(viewModel.backDispatcher)

  MainNavPage(
    component = viewModel.mainNavComponent,
    modifier = modifier,
  )
}
