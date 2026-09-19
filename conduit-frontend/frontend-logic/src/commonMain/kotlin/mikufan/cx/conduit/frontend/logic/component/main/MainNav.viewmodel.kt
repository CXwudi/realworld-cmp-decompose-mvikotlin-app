package mikufan.cx.conduit.frontend.logic.component.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import mikufan.cx.conduit.frontend.logic.component.util.MviComponent

private const val KEY_MAIN_NAV_SNAPSHOT = "main_nav_snapshot"
private const val KEY_JSON_PAYLOAD = "json_payload"

/**
 * Native CMP ViewModel owning the store created by [MainNavStoreFactory].
 *
 * Responsibilities:
 * - Persists compact [MainNavSavedSnapshot] into [SavedStateHandle].
 * - Preserves pending restored candidate if snapshot is requested before readiness.
 * - Restores snapshot upon recreation and supplies it to [MainNavStoreFactory].
 * - Exposes state as [StateFlow] and forwards [MainNavIntent]s.
 * - Disposes the store on [onCleared].
 */
class MainNavViewModel(
  storeFactory: MainNavStoreFactory,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel(), MviComponent<MainNavIntent, MainNavState> {

  private val store: Store<MainNavIntent, MainNavState, Nothing>

  init {
    val restoredSnapshot: MainNavSavedSnapshot? = runCatching {
      savedStateHandle.get<SavedState>(KEY_MAIN_NAV_SNAPSHOT)?.read {
        if (contains(KEY_JSON_PAYLOAD)) {
          val json = getString(KEY_JSON_PAYLOAD)
          Json.decodeFromString(MainNavSavedSnapshot.serializer(), json)
        } else {
          null
        }
      }
    }.getOrElse { e ->
      log.warn(e) { "Failed to restore MainNav snapshot from SavedStateHandle, using fresh state" }
      null
    }

    store = storeFactory.createStore(restoredSnapshot = restoredSnapshot)

    savedStateHandle.setSavedStateProvider(KEY_MAIN_NAV_SNAPSHOT) {
      val currentState = store.state
      val snapshotToSave = if (currentState.isReady) {
        MainNavSavedSnapshot(
          selectedTab = MainNavTab.fromMenuItem(currentState.currentMenuItem),
          accountUsername = currentState.currentUsername,
          generation = currentState.generation,
        )
      } else {
        restoredSnapshot
      }

      if (snapshotToSave != null) {
        val json = Json.encodeToString(MainNavSavedSnapshot.serializer(), snapshotToSave)
        savedState {
          putString(KEY_JSON_PAYLOAD, json)
        }
      } else {
        savedState { }
      }
    }
  }

  override val state: StateFlow<MainNavState> = store.stateFlow(viewModelScope)

  override fun send(intent: MainNavIntent) {
    store.accept(intent)
  }

  internal val isStoreDisposed: Boolean
    get() = store.isDisposed

  override fun onCleared() {
    super.onCleared()
    store.dispose()
  }
}

/**
 * Plain stateless factory for [MainNavViewModel].
 */
class MainNavViewModelFactory(
  private val storeFactory: MainNavStoreFactory,
) {
  fun create(savedStateHandle: SavedStateHandle): MainNavViewModel =
    MainNavViewModel(storeFactory, savedStateHandle)
}

private val log = KotlinLogging.logger { }
