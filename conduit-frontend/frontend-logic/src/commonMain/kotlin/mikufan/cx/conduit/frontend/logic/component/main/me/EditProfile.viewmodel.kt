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
 * Native CMP ViewModel for Edit Profile screen.
 * Wraps [EditProfileStoreFactory]'s store with initial state populated from [LoadedMe].
 * Coordinates save success / back navigation with [navigator] carrying [entryId].
 * Disposes store on clear.
 */
class EditProfileViewModel(
  storeFactory: EditProfileStoreFactory,
  initialState: EditProfileState,
  private val entryId: String,
  private val navigator: MeNavNavigator,
) : ViewModel(), MviComponent<EditProfileIntent, EditProfileState> {

  private val store: Store<EditProfileIntent, EditProfileState, EditProfileLabel> =
    storeFactory.createStore(initialState)

  override val state: StateFlow<EditProfileState> = store.stateFlow(viewModelScope)

  init {
    val navigationLabels = store.labelsChannel(viewModelScope, capacity = Channel.UNLIMITED)
    viewModelScope.launch {
      for (label in navigationLabels) {
        when (label) {
          is EditProfileLabel.SaveSuccessLabel -> navigator.onPopRoute(entryId)
          is EditProfileLabel.BackWithoutSave -> navigator.onPopRoute(entryId)
          is EditProfileLabel.Unit -> Unit
        }
      }
    }
  }

  override fun send(intent: EditProfileIntent) {
    store.accept(intent)
  }

  internal val isStoreDisposed: Boolean
    get() = store.isDisposed

  override fun onCleared() {
    store.dispose()
  }
}

class EditProfileViewModelFactory(
  private val editProfileStoreFactory: EditProfileStoreFactory,
) {
  fun create(
    loadedMe: LoadedMe,
    entryId: String,
    navigator: MeNavNavigator,
  ): EditProfileViewModel = EditProfileViewModel(
    storeFactory = editProfileStoreFactory,
    initialState = EditProfileState(
      email = loadedMe.email,
      username = loadedMe.username,
      bio = loadedMe.bio,
      imageUrl = loadedMe.imageUrl,
    ),
    entryId = entryId,
    navigator = navigator,
  )
}
