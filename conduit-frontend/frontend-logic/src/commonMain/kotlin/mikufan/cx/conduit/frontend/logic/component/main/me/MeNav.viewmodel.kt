package mikufan.cx.conduit.frontend.logic.component.main.me

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

private const val KEY_ME_NAV_SNAPSHOT = "me_nav_snapshot"
private const val KEY_JSON_PAYLOAD = "json_payload"

/**
 * Native CMP ViewModel owning the store created by [MeNavStoreFactory].
 *
 * Responsibilities:
 * - Persists compact [MeNavSavedSnapshot] (stack routes and nextEntryId) in [SavedStateHandle].
 * - Restores navigation stack on process recreation.
 * - Implements [MeNavNavigator] as a stable navigation sink for leaf ViewModels.
 * - Exposes state as [StateFlow] and disposes store on [onCleared].
 */
class MeNavViewModel(
  storeFactory: MeNavStoreFactory,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel(), MviComponent<MeNavIntent, MeNavState>, MeNavNavigator {

  private val store: Store<MeNavIntent, MeNavState, Nothing>

  init {
    val restoredSnapshot: MeNavSavedSnapshot? = runCatching {
      savedStateHandle.get<SavedState>(KEY_ME_NAV_SNAPSHOT)?.read {
        if (contains(KEY_JSON_PAYLOAD)) {
          val json = getString(KEY_JSON_PAYLOAD)
          Json.decodeFromString(MeNavSavedSnapshot.serializer(), json)
        } else {
          null
        }
      }
    }.getOrElse { e ->
      log.warn(e) { "Failed to restore MeNav snapshot from SavedStateHandle, starting from Profile" }
      null
    }

    store = storeFactory.createStore(restoredSnapshot = restoredSnapshot)

    savedStateHandle.setSavedStateProvider(KEY_ME_NAV_SNAPSHOT) {
      val currentState = store.state
      val snapshot = MeNavSavedSnapshot(
        stack = currentState.stack,
        nextEntryId = currentState.nextEntryId,
      )
      val json = Json.encodeToString(MeNavSavedSnapshot.serializer(), snapshot)
      savedState {
        putString(KEY_JSON_PAYLOAD, json)
      }
    }
  }

  override val state: StateFlow<MeNavState> = store.stateFlow(viewModelScope)

  override fun send(intent: MeNavIntent) {
    store.accept(intent)
  }

  override fun onEditProfile(loadedMe: LoadedMe) {
    send(MeNavIntent.NavigateToEditProfile(loadedMe))
  }

  override fun onAddArticle() {
    send(MeNavIntent.NavigateToAddArticle)
  }

  override fun onPopRoute(entryId: String) {
    send(MeNavIntent.PopRoute(entryId))
  }

  fun pop() {
    send(MeNavIntent.Pop)
  }

  internal val isStoreDisposed: Boolean
    get() = store.isDisposed

  override fun onCleared() {
    store.dispose()
  }
}

class MeNavViewModelFactory(
  private val meNavStoreFactory: MeNavStoreFactory,
) {
  fun create(savedStateHandle: SavedStateHandle): MeNavViewModel =
    MeNavViewModel(meNavStoreFactory, savedStateHandle)
}

private val log = KotlinLogging.logger {}
