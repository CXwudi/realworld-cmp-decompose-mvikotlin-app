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

const val KEY_LEGACY_MAIN_SAVED_STATE = "legacy_main_saved_state"
private const val KEY_JSON_PAYLOAD = "json_payload"

/**
 * Entry-owned legacy adapter ViewModel hosting an arbitrary legacy Decompose subtree [T].
 *
 * Responsibilities:
 * - Retains the legacy component and its stores across Android configuration changes (rotation).
 * - Saves and restores legacy Decompose state through native [SavedStateHandle].
 * - Bridges Essenty lifecycle with native lifecycle.
 * - Provides authoritative back dispatching via [backDispatcher].
 * - Destroys legacy stores and lifecycle when cleared.
 */
open class LegacyChildAdapterViewModel<T : Any>(
  savedStateHandle: SavedStateHandle,
  saveKey: String = KEY_LEGACY_MAIN_SAVED_STATE,
  factory: (ComponentContext) -> T,
) : ViewModel() {

  val lifecycleRegistry = LifecycleRegistry()
  val instanceKeeper = InstanceKeeperDispatcher()
  val backDispatcher = BackDispatcher()

  val stateKeeper: StateKeeperDispatcher

  val component: T

  init {
    val restoredContainer: SerializableContainer? = runCatching {
      savedStateHandle.get<SavedState>(saveKey)?.read {
        if (contains(KEY_JSON_PAYLOAD)) {
          val json = getString(KEY_JSON_PAYLOAD)
          Json.decodeFromString(SerializableContainer.serializer(), json)
        } else {
          null
        }
      }
    }.getOrElse { e ->
      log.warn(e) { "Failed to restore legacy state for $saveKey from SavedStateHandle, using fresh state" }
      null
    }

    stateKeeper = StateKeeperDispatcher(restoredContainer)

    savedStateHandle.setSavedStateProvider(saveKey) {
      val container = stateKeeper.save()
      val json = Json.encodeToString(SerializableContainer.serializer(), container)
      savedState {
        putString(KEY_JSON_PAYLOAD, json)
      }
    }

    val componentContext: ComponentContext = DefaultComponentContext(
      lifecycle = lifecycleRegistry,
      stateKeeper = stateKeeper,
      instanceKeeper = instanceKeeper,
      backHandler = backDispatcher,
    )

    component = factory(componentContext)
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
   * Stops the Essenty lifecycle without destroying the retained component.
   */
  fun onHostDisposed() {
    if (lifecycleRegistry.state != com.arkivanov.essenty.lifecycle.Lifecycle.State.DESTROYED) {
      lifecycleRegistry.stop()
    }
  }

  override fun onCleared() {
    super.onCleared()
    if (lifecycleRegistry.state != com.arkivanov.essenty.lifecycle.Lifecycle.State.DESTROYED) {
      lifecycleRegistry.destroy()
    }
    instanceKeeper.destroy()
  }
}

/**
 * Plain stateless factory for [LegacyChildAdapterViewModel].
 */
class LegacyChildAdapterViewModelFactory {
  fun <T : Any> create(
    savedStateHandle: SavedStateHandle,
    saveKey: String,
    factory: (ComponentContext) -> T,
  ): LegacyChildAdapterViewModel<T> =
    LegacyChildAdapterViewModel(savedStateHandle, saveKey, factory)
}

/**
 * Entry-owned legacy adapter ViewModel hosting the existing Main Decompose subtree.
 * Retained for Phase 1 compatibility.
 */
class LegacyMainAdapterViewModel(
  mainNavComponentFactory: MainNavComponentFactory,
  savedStateHandle: SavedStateHandle,
) : LegacyChildAdapterViewModel<MainNavComponent>(
  savedStateHandle = savedStateHandle,
  saveKey = KEY_LEGACY_MAIN_SAVED_STATE,
  factory = { ctx -> mainNavComponentFactory.create(ctx) },
) {
  val mainNavComponent: MainNavComponent
    get() = component
}

/**
 * Plain stateless factory for [LegacyMainAdapterViewModel].
 */
class LegacyMainAdapterViewModelFactory(
  private val mainNavComponentFactory: MainNavComponentFactory,
) {
  fun create(savedStateHandle: SavedStateHandle): LegacyMainAdapterViewModel =
    LegacyMainAdapterViewModel(mainNavComponentFactory, savedStateHandle)
}

private val log = KotlinLogging.logger { }
