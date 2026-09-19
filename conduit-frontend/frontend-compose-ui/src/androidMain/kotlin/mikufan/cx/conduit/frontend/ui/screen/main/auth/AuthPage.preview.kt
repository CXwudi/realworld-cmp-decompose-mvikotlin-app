package mikufan.cx.conduit.frontend.ui.screen.main.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageMode
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageState
import mikufan.cx.conduit.frontend.ui.util.SetupPreviewUI

@Preview
@Composable
fun MainPageLoginPreview() {
  SetupPreviewUI {
    AuthPage(
      stateFlow = MutableStateFlow(
        AuthPageState("my username", "my password", "my email", AuthPageMode.SIGN_IN),
      ),
      labelsFlow = emptyFlow(),
      onSend = {},
    )
  }
}

@Preview
@Composable
fun MainPageRegisterPreview() {
  SetupPreviewUI {
    AuthPage(
      stateFlow = MutableStateFlow(
        AuthPageState("my username", "my password", "my email", AuthPageMode.REGISTER),
      ),
      labelsFlow = emptyFlow(),
      onSend = {},
    )
  }
}
