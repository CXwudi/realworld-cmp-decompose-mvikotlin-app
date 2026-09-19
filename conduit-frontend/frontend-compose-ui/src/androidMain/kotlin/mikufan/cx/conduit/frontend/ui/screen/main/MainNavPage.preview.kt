package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import mikufan.cx.conduit.frontend.logic.component.main.MainNavState
import mikufan.cx.conduit.frontend.ui.util.SetupPreviewUI

@Composable
@Preview
fun MainPagePreview() {
  SetupPreviewUI {
    MainNavScaffold(
      state = MutableStateFlow(MainNavState.notLoggedIn()),
      onSend = {},
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Feed Content")
      }
    }
  }
}

@Composable
@Preview
fun MainPagePreviewForLoginUser() {
  SetupPreviewUI {
    MainNavScaffold(
      state = MutableStateFlow(MainNavState.loggedIn("testuser")),
      onSend = {},
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Feed Content (Logged In)")
      }
    }
  }
}
