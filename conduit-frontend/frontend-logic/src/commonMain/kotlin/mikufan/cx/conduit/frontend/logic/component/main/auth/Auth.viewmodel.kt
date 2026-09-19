package mikufan.cx.conduit.frontend.logic.component.main.auth

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
 * Native CMP ViewModel wrapping the store created by [AuthPageStoreFactory].
 *
 * Owns exactly one store, exposes its state as [StateFlow] and labels as [Flow],
 * forwards intents to the store, and disposes the store on [onCleared].
 */
class AuthViewModel(
  storeFactory: AuthPageStoreFactory,
) : ViewModel(),
  MviComponent<AuthPageIntent, AuthPageState>,
  LabelEmitter<AuthPageLabel> {

  private val store: Store<AuthPageIntent, AuthPageState, AuthPageLabel> =
    storeFactory.createStore()

  override val state: StateFlow<AuthPageState> = store.stateFlow(viewModelScope)
  override val labels: Flow<AuthPageLabel> = store.labels

  override fun send(intent: AuthPageIntent) {
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
 * Plain stateless factory for [AuthViewModel].
 */
class AuthViewModelFactory(
  private val storeFactory: AuthPageStoreFactory,
) {
  fun create(): AuthViewModel = AuthViewModel(storeFactory)
}
