package mikufan.cx.conduit.frontend.ui.screen.main.me

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageComponent
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageIntent
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageState
import mikufan.cx.conduit.frontend.ui.util.SetupPreviewUI

@Composable
@Preview
fun MePagePreview() {
  setSingletonImageLoaderFactory { context ->
    ImageLoader.Builder(context)
      .crossfade(true)
      .build()
  }
  SetupPreviewUI {
    val mePageComponent = object : MePageComponent {
      override val state: StateFlow<MePageState> = MutableStateFlow(MePageState.Loading)

      override fun send(intent: MePageIntent) {}

    }
    MePage(mePageComponent)
  }
}