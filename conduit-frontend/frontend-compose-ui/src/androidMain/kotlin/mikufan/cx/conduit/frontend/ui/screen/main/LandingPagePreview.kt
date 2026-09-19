package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import mikufan.cx.conduit.frontend.logic.component.landing.LandingPageLabel
import mikufan.cx.conduit.frontend.logic.component.landing.LandingPageState
import mikufan.cx.conduit.frontend.ui.screen.LandingPage
import mikufan.cx.conduit.frontend.ui.util.SetupPreviewUI

@Composable
@Preview
fun LandingPagePreview() {
  SetupPreviewUI {
    LandingPage(
      state = MutableStateFlow(LandingPageState("https://conduit.productionready.io/api")),
      labels = MutableStateFlow(LandingPageLabel.Failure("some error")),
      onSend = {},
    )
  }
}
