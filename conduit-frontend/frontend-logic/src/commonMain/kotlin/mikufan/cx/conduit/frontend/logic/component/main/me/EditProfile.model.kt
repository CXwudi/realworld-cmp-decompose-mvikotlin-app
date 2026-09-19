package mikufan.cx.conduit.frontend.logic.component.main.me

data class EditProfileState(
  val email: String,
  val username: String,
  val bio: String,
  val imageUrl: String,
  /**
   * use empty string to indicate no change to password
   */
  val password: String = "",
  /**
   * error message if any, empty string means no error
   */
  val errorMsg: String = "",
)

sealed interface EditProfileIntent {
  data class EmailChanged(val email: String) : EditProfileIntent
  data class UsernameChanged(val username: String) : EditProfileIntent
  data class BioChanged(val bio: String) : EditProfileIntent
  data class ImageUrlChanged(val imageUrl: String) : EditProfileIntent
  data class PasswordChanged(val password: String) : EditProfileIntent
  data object Save : EditProfileIntent
  data object BackWithoutSave : EditProfileIntent
}

sealed interface EditProfileLabel {
  data object SaveSuccessLabel : EditProfileLabel
  data object BackWithoutSave : EditProfileLabel
  /**
   * Just for test purpose
   */
  data object Unit : EditProfileLabel
}

