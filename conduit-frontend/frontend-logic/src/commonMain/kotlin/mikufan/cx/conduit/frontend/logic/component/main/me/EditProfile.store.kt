package mikufan.cx.conduit.frontend.logic.component.main.me

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mikufan.cx.conduit.common.UserUpdateDto
import mikufan.cx.conduit.frontend.logic.component.util.rethrowIfShouldNotBeHandled
import mikufan.cx.conduit.frontend.logic.service.main.EditProfileService

class EditProfileStoreFactory(
  private val storeFactory: StoreFactory,
  private val editProfileService: EditProfileService,
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

  private val executorFactory = coroutineExecutorFactory<EditProfileIntent, Nothing, EditProfileState, Msg, EditProfileLabel>(mainDispatcher) {
    onIntent<EditProfileIntent.EmailChanged> {
      dispatch(Msg.EmailChanged(it.email))
    }

    onIntent<EditProfileIntent.UsernameChanged> {
      dispatch(Msg.UsernameChanged(it.username))
    }

    onIntent<EditProfileIntent.BioChanged> {
      dispatch(Msg.BioChanged(it.bio))
    }

    onIntent<EditProfileIntent.ImageUrlChanged> {
      dispatch(Msg.ImageUrlChanged(it.imageUrl))
    }

    onIntent<EditProfileIntent.PasswordChanged> {
      dispatch(Msg.PasswordChanged(it.password))
    }

    onIntent<EditProfileIntent.Save> {
      launch {
        try {
          withContext(Dispatchers.Default) {
            editProfileService.updateAndSave(
              UserUpdateDto(
                email = state().email,
                username = state().username,
                password = state().password.ifEmpty { null },
                image = state().imageUrl,
                bio = state().bio,
              )
            )
          }
          
          publish(EditProfileLabel.SaveSuccessLabel)
        } catch (t: Throwable) {
          rethrowIfShouldNotBeHandled(t) { e ->
            log.error(e) { "Failed to update profile" }
            dispatch(Msg.ShowErrorMsg(e.message ?: "Unknown error"))
          }
        }
      }
    }

    onIntent<EditProfileIntent.BackWithoutSave> {
      publish(EditProfileLabel.BackWithoutSave)
    }
  }

  private val reducer: Reducer<EditProfileState, Msg> = Reducer { msg ->
    when (msg) {
      is Msg.EmailChanged -> this.copy(email = msg.email, errorMsg = "")
      is Msg.UsernameChanged -> this.copy(username = msg.username, errorMsg = "")
      is Msg.BioChanged -> this.copy(bio = msg.bio, errorMsg = "")
      is Msg.ImageUrlChanged -> this.copy(imageUrl = msg.imageUrl, errorMsg = "")
      is Msg.PasswordChanged -> this.copy(password = msg.password, errorMsg = "")
      is Msg.ShowErrorMsg -> this.copy(errorMsg = msg.errorMsg)
    }
  }

  fun createStore(initialState: EditProfileState) = storeFactory.create(
    name = "EditProfileStore",
    initialState = initialState,
    executorFactory = executorFactory,
    reducer = reducer
  )

  private sealed interface Msg {
    data class EmailChanged(val email: String) : Msg
    data class UsernameChanged(val username: String) : Msg
    data class BioChanged(val bio: String) : Msg
    data class ImageUrlChanged(val imageUrl: String) : Msg
    data class PasswordChanged(val password: String) : Msg
    data class ShowErrorMsg(val errorMsg: String) : Msg
  }
}

private val log = KotlinLogging.logger {}