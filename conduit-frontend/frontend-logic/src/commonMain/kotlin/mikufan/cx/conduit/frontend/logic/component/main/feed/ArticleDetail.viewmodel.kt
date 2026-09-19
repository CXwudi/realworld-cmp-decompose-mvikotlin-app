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
 * Native CMP ViewModel for Article Detail screen.
 * Wraps [ArticleDetailStoreFactory]'s store.
 * Subscribes synchronously to store labels via [labelsChannel] during [init],
 * forwarding back-to-list navigation with [entryId] to [navigator] for stale-event protection.
 * Disposes the store on [onCleared].
 */
class ArticleDetailViewModel(
  storeFactory: ArticleDetailStoreFactory,
  basicInfo: ArticleBasicInfo,
  private val entryId: String,
  private val navigator: ArticlesNavNavigator,
) : ViewModel(), MviComponent<ArticleDetailIntent, ArticleDetailState> {

  private val store: Store<ArticleDetailIntent, ArticleDetailState, ArticleDetailLabel> =
    storeFactory.createStore(basicInfo, autoInit = true)

  private val _labels = MutableSharedFlow<ArticleDetailLabel>(extraBufferCapacity = 64)
  val labels: Flow<ArticleDetailLabel> = _labels.asSharedFlow()

  override val state: StateFlow<ArticleDetailState> = store.stateFlow(viewModelScope)

  init {
    val labelsChannel = store.labelsChannel(viewModelScope, capacity = Channel.UNLIMITED)
    viewModelScope.launch {
      for (label in labelsChannel) {
        when (label) {
          is ArticleDetailLabel.BackToList -> navigator.onCloseDetail(entryId)
          is ArticleDetailLabel.Failure -> _labels.tryEmit(label)
        }
      }
    }
  }

  override fun send(intent: ArticleDetailIntent) {
    store.accept(intent)
  }

  internal val isStoreDisposed: Boolean
    get() = store.isDisposed

  override fun onCleared() {
    store.dispose()
  }
}

class ArticleDetailViewModelFactory(
  private val articleDetailStoreFactory: ArticleDetailStoreFactory,
) {
  fun create(
    basicInfo: ArticleBasicInfo,
    entryId: String,
    navigator: ArticlesNavNavigator,
  ): ArticleDetailViewModel = ArticleDetailViewModel(
    storeFactory = articleDetailStoreFactory,
    basicInfo = basicInfo,
    entryId = entryId,
    navigator = navigator,
  )
}
