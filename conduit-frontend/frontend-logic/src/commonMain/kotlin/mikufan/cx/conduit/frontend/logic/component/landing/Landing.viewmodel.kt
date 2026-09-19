package mikufan.cx.conduit.frontend.logic.component.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mikufan.cx.conduit.frontend.logic.component.util.LabelEmitter
import mikufan.cx.conduit.frontend.logic.component.util.MviComponent

/**
 * Native CMP ViewModel wrapping the store created by [LandingPageStoreFactory].
 *
 * Owns exactly one store, exposes its state as [StateFlow] using [viewModelScope],
 * forwards intents to the store, and disposes the store on [onCleared].
 */
class LandingViewModel(
  storeFactory: LandingPageStoreFactory,
) : ViewModel(),
  MviComponent<LandingPageIntent, LandingPageState>,
  LabelEmitter<LandingPageLabel> {

  private val store: Store<LandingPageIntent, LandingPageState, LandingPageLabel> =
    storeFactory.createStore()

  override val state: StateFlow<LandingPageState> = store.stateFlow(viewModelScope)
  override val labels: Flow<LandingPageLabel> = store.labels

  override fun send(intent: LandingPageIntent) {
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
 * Plain stateless factory for [LandingViewModel].
 */
class LandingViewModelFactory(
  private val storeFactory: LandingPageStoreFactory,
) {
  fun create(): LandingViewModel = LandingViewModel(storeFactory)
}
