package mikufan.cx.conduit.frontend.logic.component.main.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.labelsChannel
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mikufan.cx.conduit.frontend.logic.component.util.MviComponent

/**
 * Native CMP ViewModel for Add Article screen.
 * Wraps [AddArticleStoreFactory]'s store.
 * Coordinates publish success / back navigation with [navigator] carrying [entryId].
 * Disposes store on clear.
 */
class AddArticleViewModel(
  storeFactory: AddArticleStoreFactory,
  private val entryId: String,
  private val navigator: MeNavNavigator,
) : ViewModel(), MviComponent<AddArticleIntent, AddArticleState> {

  private val store: Store<AddArticleIntent, AddArticleState, AddArticleLabel> =
    storeFactory.createStore()

  override val state: StateFlow<AddArticleState> = store.stateFlow(viewModelScope)

  init {
    val navigationLabels = store.labelsChannel(viewModelScope, capacity = Channel.UNLIMITED)
    viewModelScope.launch {
      for (label in navigationLabels) {
        when (label) {
          is AddArticleLabel.PublishSuccess -> navigator.onPopRoute(entryId)
          is AddArticleLabel.BackWithoutPublish -> navigator.onPopRoute(entryId)
        }
      }
    }
  }

  override fun send(intent: AddArticleIntent) {
    store.accept(intent)
  }

  internal val isStoreDisposed: Boolean
    get() = store.isDisposed

  override fun onCleared() {
    store.dispose()
  }
}

class AddArticleViewModelFactory(
  private val addArticleStoreFactory: AddArticleStoreFactory,
) {
  fun create(
    entryId: String,
    navigator: MeNavNavigator,
  ): AddArticleViewModel = AddArticleViewModel(
    storeFactory = addArticleStoreFactory,
    entryId = entryId,
    navigator = navigator,
  )
}
