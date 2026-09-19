package mikufan.cx.conduit.frontend.ui.screen.main.me

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import mikufan.cx.conduit.frontend.logic.component.main.me.EditProfileState
import mikufan.cx.conduit.frontend.ui.util.SetupPreviewUI

@Composable
@Preview
fun EditProfilePreview() {
  SetupPreviewUI {
    EditProfilePage(
      stateFlow = MutableStateFlow(
        EditProfileState(
          email = "email",
          username = "username",
          bio = "bio",
          imageUrl = "imageUrl",
          password = "password2",
          errorMsg = "some error",
        ),
      ),
      onSend = {},
    )
  }
}
