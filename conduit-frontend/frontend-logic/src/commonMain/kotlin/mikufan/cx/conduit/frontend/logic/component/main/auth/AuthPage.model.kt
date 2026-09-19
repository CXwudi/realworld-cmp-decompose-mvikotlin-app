package mikufan.cx.conduit.frontend.logic.component.main.auth

data class AuthPageState(
  val username: String,
  val password: String,
  val email: String,
  val mode: AuthPageMode
)

enum class AuthPageMode {
  SIGN_IN,
  REGISTER;

  val opposite: AuthPageMode
    get() = when (this) {
      SIGN_IN -> REGISTER
      REGISTER -> SIGN_IN
    }
}

sealed interface AuthPageIntent {
  data class UsernameChanged(val username: String) : AuthPageIntent
  data class PasswordChanged(val password: String) : AuthPageIntent
  data class EmailChanged(val email: String) : AuthPageIntent

  /**
   * Switch between login and register
   */
  data object SwitchMode : AuthPageIntent

  /**
   * Intent used for both starting the actual login or register flow
   */
  data object AuthAction : AuthPageIntent

  /**
   * Going back to the landing page
   */
  data object BackToLanding : AuthPageIntent
}

sealed interface AuthPageLabel {
  data class Failure(
    val exception: Exception? = null,
    val message: String,
  ) : AuthPageLabel
  /**
   * Just for test purpose
   */
  data object BackToLanding : AuthPageLabel
}