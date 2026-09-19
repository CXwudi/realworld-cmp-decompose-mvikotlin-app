package mikufan.cx.conduit.frontend.logic.component.main.feed

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

private const val KEY_ARTICLES_NAV_SNAPSHOT = "articles_nav_snapshot"
private const val KEY_JSON_PAYLOAD = "json_payload"

/**
 * Native CMP ViewModel owning the navigation store created by [ArticlesNavStoreFactory].
 *
 * Responsibilities:
 * - Persists compact [ArticlesNavSavedSnapshot] (stack routes and nextEntryId) in [SavedStateHandle].
 * - Restores navigation stack across Android recreation.
 * - Implements [ArticlesNavNavigator] as a stable navigation sink for leaf ViewModels.
 * - Exposes state as [StateFlow] and disposes store on [onCleared].
 */
class ArticlesNavViewModel(
  storeFactory: ArticlesNavStoreFactory,
  private val savedStateHandle: SavedStateHandle,
  val searchFilter: ArticlesSearchFilter,
) : ViewModel(), MviComponent<ArticlesNavIntent, ArticlesNavState>, ArticlesNavNavigator {

  private val store: Store<ArticlesNavIntent, ArticlesNavState, Nothing>

  init {
    val restoredSnapshot: ArticlesNavSavedSnapshot? = runCatching {
      savedStateHandle.get<SavedState>(KEY_ARTICLES_NAV_SNAPSHOT)?.read {
        if (contains(KEY_JSON_PAYLOAD)) {
          val json = getString(KEY_JSON_PAYLOAD)
          Json.decodeFromString(ArticlesNavSavedSnapshot.serializer(), json)
        } else {
          null
        }
      }
    }.getOrElse { e ->
      log.warn(e) { "Failed to restore ArticlesNav snapshot from SavedStateHandle; falling back to root list" }
      null
    }

    store = storeFactory.createStore(restoredSnapshot = restoredSnapshot)

    savedStateHandle.setSavedStateProvider(KEY_ARTICLES_NAV_SNAPSHOT) {
      val currentState = store.state
      val snapshot = ArticlesNavSavedSnapshot(
        stack = currentState.stack,
        nextEntryId = currentState.nextEntryId,
      )
      val json = Json.encodeToString(ArticlesNavSavedSnapshot.serializer(), snapshot)
      savedState {
        putString(KEY_JSON_PAYLOAD, json)
      }
    }
  }

  override val state: StateFlow<ArticlesNavState> = store.stateFlow(viewModelScope)

  override fun send(intent: ArticlesNavIntent) {
    store.accept(intent)
  }

  override fun onOpenArticle(basicInfo: ArticleBasicInfo) {
    store.accept(ArticlesNavIntent.OpenArticle(basicInfo))
  }

  override fun onCloseDetail(entryId: String) {
    store.accept(ArticlesNavIntent.CloseDetail(entryId))
  }

  fun pop() {
    store.accept(ArticlesNavIntent.Pop)
  }

  internal val isStoreDisposed: Boolean
    get() = store.isDisposed

  override fun onCleared() {
    store.dispose()
  }
}

class ArticlesNavViewModelFactory(
  private val storeFactory: ArticlesNavStoreFactory,
) {
  fun create(
    savedStateHandle: SavedStateHandle,
    searchFilter: ArticlesSearchFilter,
  ): ArticlesNavViewModel = ArticlesNavViewModel(
    storeFactory = storeFactory,
    savedStateHandle = savedStateHandle,
    searchFilter = searchFilter,
  )
}

private val log = KotlinLogging.logger {}
