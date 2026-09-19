package mikufan.cx.conduit.frontend.logic.component.main.auth

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mikufan.cx.conduit.frontend.logic.component.util.rethrowIfShouldNotBeHandled
import mikufan.cx.conduit.frontend.logic.service.main.AuthService

class AuthPageStoreFactory(
  private val storeFactory: StoreFactory,
  private val authService: AuthService,
  dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

  private val executorFactory =
    coroutineExecutorFactory<AuthPageIntent, Nothing, AuthPageState, Msg, AuthPageLabel>(dispatcher) {
      onIntent<AuthPageIntent.UsernameChanged> {
        dispatch(Msg.UsernameChanged(it.username))
      }
      onIntent<AuthPageIntent.PasswordChanged> {
        dispatch(Msg.PasswordChanged(it.password))
      }
      onIntent<AuthPageIntent.EmailChanged> {
        dispatch(Msg.EmailChanged(it.email))
      }
      onIntent<AuthPageIntent.SwitchMode> {
        val state = state()
        dispatch(Msg.SwitchMode(state.mode.opposite))
      }
      onIntent<AuthPageIntent.AuthAction> {
        val state = state()

        launch {
          try {
            withContext(Dispatchers.Default) {
              if (state.mode == AuthPageMode.SIGN_IN) {
                authService.login(state.email, state.password)
              } else {
                authService.register(state.email, state.username, state.password)
              }
            }
          } catch (t: Throwable) {
            rethrowIfShouldNotBeHandled(t) { e ->
              log.error(e) { "Failed to login" }
              publish(
                AuthPageLabel.Failure(
                  e as? Exception,
                  e.message ?: "Unknown error happened while authenticating")
              )
            }
          }

        }

      }

      onIntent<AuthPageIntent.BackToLanding> {
        launch {
          withContext(Dispatchers.Default) {
            authService.reset()
          }
          publish(AuthPageLabel.BackToLanding)
        }
      }
    }

  private val reducer = Reducer<AuthPageState, Msg> { msg ->
    when (msg) {
      is Msg.UsernameChanged -> this.copy(username = msg.username)
      is Msg.PasswordChanged -> this.copy(password = msg.password)
      is Msg.EmailChanged -> this.copy(email = msg.email)
      is Msg.SwitchMode -> this.copy(mode = msg.mode, email = "", password = "", username = "")
    }
  }

  fun createStore() = storeFactory.create(
    name = "AuthPageStore",
    initialState = AuthPageState("", "", "", AuthPageMode.SIGN_IN),
    executorFactory = executorFactory,
    reducer = reducer
  )

  private sealed interface Msg {
    data class UsernameChanged(val username: String) : Msg
    data class PasswordChanged(val password: String) : Msg
    data class EmailChanged(val email: String) : Msg
    data class SwitchMode(val mode: AuthPageMode) : Msg

  }

}

private val log = KotlinLogging.logger {}