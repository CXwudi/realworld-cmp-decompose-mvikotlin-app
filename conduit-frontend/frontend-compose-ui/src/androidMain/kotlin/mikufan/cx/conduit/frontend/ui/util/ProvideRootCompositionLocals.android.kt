package mikufan.cx.conduit.frontend.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun ProvideRootCompositionLocals(content: @Composable () -> Unit) {
  // Pass-through: Android ComponentActivity hosts both LocalViewModelStoreOwner and LocalSavedStateRegistryOwner
  content()
}
