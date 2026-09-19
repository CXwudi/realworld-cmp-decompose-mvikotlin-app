package mikufan.cx.conduit.frontend.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun ProvideRootCompositionLocals(content: @Composable () -> Unit) {
  DefaultRootCompositionLocalsProvider(content)
}
