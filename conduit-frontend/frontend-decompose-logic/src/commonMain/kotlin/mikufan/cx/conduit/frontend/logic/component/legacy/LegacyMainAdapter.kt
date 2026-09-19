package mikufan.cx.conduit.frontend.logic.component.legacy

import androidx.lifecycle.Lifecycle as AndroidxLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.essenty.lifecycle.stop
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponent
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponentFactory

private const val KEY_LEGACY_MAIN_SAVED_STATE = "legacy_main_saved_state"
private const val KEY_JSON_PAYLOAD = "json_payload"

/**
 * Entry-owned legacy adapter ViewModel hosting the existing Main Decompose subtree.
 *
 * Responsibilities:
 * - Retains the legacy component and its stores across Android configuration changes (rotation).
 * - Saves and restores legacy Decompose state through native [SavedStateHandle].
 * - Bridges Essenty lifecycle with native lifecycle.
 * - Provides authoritative back dispatching via [backDispatcher].
 * - Destroys legacy stores and lifecycle when cleared.
 */
class LegacyMainAdapterViewModel(
  mainNavComponentFactory: MainNavComponentFactory,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val lifecycleRegistry = LifecycleRegistry()
  val instanceKeeper = InstanceKeeperDispatcher()
  val backDispatcher = BackDispatcher()

  val stateKeeper: StateKeeperDispatcher

  val mainNavComponent: MainNavComponent

  init {
    // 1. Attempt to restore state from SavedStateHandle
    val restoredContainer: SerializableContainer? = runCatching {
      savedStateHandle.get<SavedState>(KEY_LEGACY_MAIN_SAVED_STATE)?.read {
        if (contains(KEY_JSON_PAYLOAD)) {
          val json = getString(KEY_JSON_PAYLOAD)
          Json.decodeFromString(SerializableContainer.serializer(), json)
        } else {
          null
        }
      }
    }.getOrElse { e ->
      log.warn(e) { "Failed to restore legacy Main state from SavedStateHandle, using fresh state" }
      null
    }

    stateKeeper = StateKeeperDispatcher(restoredContainer)

    // 2. Register saved state provider to write state snapshots to SavedStateHandle
    savedStateHandle.setSavedStateProvider(KEY_LEGACY_MAIN_SAVED_STATE) {
      val container = stateKeeper.save()
      val json = Json.encodeToString(SerializableContainer.serializer(), container)
      savedState {
        putString(KEY_JSON_PAYLOAD, json)
      }
    }

    // 3. Construct DefaultComponentContext
    val componentContext: ComponentContext = DefaultComponentContext(
      lifecycle = lifecycleRegistry,
      stateKeeper = stateKeeper,
      instanceKeeper = instanceKeeper,
      backHandler = backDispatcher,
    )

    // 4. Create the legacy Main subtree
    mainNavComponent = mainNavComponentFactory.create(componentContext)
  }

  fun handleBack(): Boolean = backDispatcher.back()

  /**
   * Synchronizes native Lifecycle events to Essenty's [lifecycleRegistry].
   * Guarded against already destroyed state to ensure clear-before-disposal ordering does not throw.
   */
  fun syncLifecycleEvent(event: AndroidxLifecycle.Event) {
    if (lifecycleRegistry.state == com.arkivanov.essenty.lifecycle.Lifecycle.State.DESTROYED) {
      return
    }
    when (event) {
      AndroidxLifecycle.Event.ON_CREATE -> lifecycleRegistry.create()
      AndroidxLifecycle.Event.ON_START -> lifecycleRegistry.start()
      AndroidxLifecycle.Event.ON_RESUME -> lifecycleRegistry.resume()
      AndroidxLifecycle.Event.ON_PAUSE -> lifecycleRegistry.pause()
      AndroidxLifecycle.Event.ON_STOP -> lifecycleRegistry.stop()
      AndroidxLifecycle.Event.ON_DESTROY -> {
        // Native DESTROY during Android rotation must not destroy the retained legacy tree.
        // True destruction happens only when onCleared() is invoked.
        if (lifecycleRegistry.state != com.arkivanov.essenty.lifecycle.Lifecycle.State.DESTROYED) {
          lifecycleRegistry.stop()
        }
      }
      AndroidxLifecycle.Event.ON_ANY -> {}
    }
  }

  /**
   * Called when the Composable host leaves composition.
   * Stops the legacy lifecycle without destroying it.
   */
  fun onHostDisposed() {
    if (lifecycleRegistry.state != com.arkivanov.essenty.lifecycle.Lifecycle.State.DESTROYED) {
      lifecycleRegistry.stop()
    }
  }

  override fun onCleared() {
    super.onCleared()
    log.debug { "Clearing LegacyMainAdapterViewModel: destroying legacy lifecycle and instance keeper" }
    lifecycleRegistry.destroy()
    instanceKeeper.destroy()
  }
}

/**
 * Plain assisted factory for creating [LegacyMainAdapterViewModel].
 */
class LegacyMainAdapterViewModelFactory(
  private val mainNavComponentFactory: MainNavComponentFactory,
) {
  fun create(savedStateHandle: SavedStateHandle): LegacyMainAdapterViewModel =
    LegacyMainAdapterViewModel(mainNavComponentFactory, savedStateHandle)
}

private val log = KotlinLogging.logger { }
