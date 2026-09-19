package mikufan.cx.conduit.frontend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import mikufan.cx.conduit.frontend.logic.AppDependencies
import mikufan.cx.conduit.frontend.ui.screen.RootNavigation
import mikufan.cx.conduit.frontend.ui.util.ProvideRootCompositionLocals
import mikufan.cx.conduit.frontend.ui.util.SetupUI

/**
 * Top-level application setup and entry point.
 * Initializes singleton ImageLoader and renders [MainUI] with plain [AppDependencies].
 * Free of any Koin types or Koin context wrappers.
 */
@Composable
fun setupAndStartMainUI(
  dependencies: AppDependencies,
) {
  setSingletonImageLoaderFactory { context ->
    ImageLoader.Builder(context)
      .components {
        add(SvgDecoder.Factory())
      }
      .crossfade(true)
      .build()
  }
  MainUI(dependencies)
}

/**
 * Root UI surface hosting [RootNavigation].
 */
@Composable
fun MainUI(
  dependencies: AppDependencies,
) {
  ProvideRootCompositionLocals {
    SetupUI {
      Surface(
        Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
      ) {
        RootNavigation(dependencies)
      }
    }
  }
}
