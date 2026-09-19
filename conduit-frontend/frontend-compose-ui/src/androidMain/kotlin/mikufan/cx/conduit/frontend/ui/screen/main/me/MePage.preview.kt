package mikufan.cx.conduit.frontend.ui.screen.main.me

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import kotlinx.coroutines.flow.MutableStateFlow
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
    MePage(
      state = MutableStateFlow(MePageState.Loading),
      onSend = {},
    )
  }
}
