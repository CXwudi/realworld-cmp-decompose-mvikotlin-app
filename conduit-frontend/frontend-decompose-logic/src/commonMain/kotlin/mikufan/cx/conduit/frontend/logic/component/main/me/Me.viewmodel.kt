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
 * Native CMP ViewModel for Me (Profile) screen.
 * Wraps [MeStoreFactory]'s store, coordinates navigation labels with [navigator],
 * and disposes store on clear.
 */
class MePageViewModel(
  storeFactory: MeStoreFactory,
  private val navigator: MeNavNavigator,
) : ViewModel(), MviComponent<MePageIntent, MePageState> {

  private val store: Store<MePageIntent, MePageState, MePageLabel> =
    storeFactory.createStore()

  override val state: StateFlow<MePageState> = store.stateFlow(viewModelScope)

  init {
    val navigationLabels = store.labelsChannel(viewModelScope, capacity = Channel.UNLIMITED)
    viewModelScope.launch {
      for (label in navigationLabels) {
        when (label) {
          is MePageLabel.EditProfile -> navigator.onEditProfile(label.loadedMe)
          is MePageLabel.AddArticle -> navigator.onAddArticle()
          is MePageLabel.TestOnly -> Unit
        }
      }
    }
  }

  override fun send(intent: MePageIntent) {
    store.accept(intent)
  }

  internal val isStoreDisposed: Boolean
    get() = store.isDisposed

  override fun onCleared() {
    store.dispose()
  }
}

class MePageViewModelFactory(
  private val meStoreFactory: MeStoreFactory,
) {
  fun create(navigator: MeNavNavigator): MePageViewModel =
    MePageViewModel(meStoreFactory, navigator)
}
