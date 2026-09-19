package mikufan.cx.conduit.frontend.logic.component.main.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.labelsChannel
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import mikufan.cx.conduit.frontend.logic.component.util.MviComponent

/**
 * Native CMP ViewModel for the Articles List screen.
 * Wraps [ArticlesListStoreFactory]'s store.
 * Subscribes synchronously to store labels via [labelsChannel] during [init],
 * forwarding article open requests to [navigator] and buffering UI error messages.
 * Disposes the store on [onCleared].
 */
class ArticlesListViewModel(
  storeFactory: ArticlesListStoreFactory,
  searchFilter: ArticlesSearchFilter,
  private val navigator: ArticlesNavNavigator,
) : ViewModel(), MviComponent<ArticlesListIntent, ArticlesListState> {

  private val store: Store<ArticlesListIntent, ArticlesListState, ArticlesListLabel> =
    storeFactory.create(searchFilter)

  private val _labels = MutableSharedFlow<ArticlesListLabel>(extraBufferCapacity = 64)
  val labels: Flow<ArticlesListLabel> = _labels.asSharedFlow()

  override val state: StateFlow<ArticlesListState> = store.stateFlow(viewModelScope)

  init {
    val labelsChannel = store.labelsChannel(viewModelScope, capacity = Channel.UNLIMITED)
    viewModelScope.launch {
      for (label in labelsChannel) {
        when (label) {
          is ArticlesListLabel.OpenArticle -> navigator.onOpenArticle(label.basicInfo)
          is ArticlesListLabel.Failure -> _labels.tryEmit(label)
        }
      }
    }
  }

  override fun send(intent: ArticlesListIntent) {
    store.accept(intent)
  }

  internal val isStoreDisposed: Boolean
    get() = store.isDisposed

  override fun onCleared() {
    store.dispose()
  }
}

class ArticlesListViewModelFactory(
  private val articlesListStoreFactory: ArticlesListStoreFactory,
) {
  fun create(
    searchFilter: ArticlesSearchFilter,
    navigator: ArticlesNavNavigator,
  ): ArticlesListViewModel = ArticlesListViewModel(
    storeFactory = articlesListStoreFactory,
    searchFilter = searchFilter,
    navigator = navigator,
  )
}
