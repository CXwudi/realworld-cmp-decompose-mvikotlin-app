package mikufan.cx.conduit.frontend.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import mikufan.cx.conduit.frontend.logic.AppDependencies
import mikufan.cx.conduit.frontend.logic.component.landing.LandingViewModel
import mikufan.cx.conduit.frontend.logic.component.legacy.LegacyMainAdapterViewModel
import mikufan.cx.conduit.frontend.logic.component.root.RootRoute
import mikufan.cx.conduit.frontend.logic.component.root.RootViewModel
import mikufan.cx.conduit.frontend.ui.screen.main.LegacyMainPage

/**
 * Root navigation host using Navigation 3 and native ViewModels.
 *
 * Driven by [RootViewModel] derived from KStore.
 * Initial loading is rendered outside [NavDisplay] to ensure restored entry state
 * is not discarded while the authoritative gate initializes.
 */
@Composable
fun RootNavigation(
  dependencies: AppDependencies,
  modifier: Modifier = Modifier,
) {
  val rootViewModel: RootViewModel = viewModel {
    dependencies.rootViewModelFactory.create()
  }
  RootNavigation(
    rootViewModel = rootViewModel,
    dependencies = dependencies,
    modifier = modifier,
  )
}

@Composable
fun RootNavigation(
  rootViewModel: RootViewModel,
  dependencies: AppDependencies,
  modifier: Modifier = Modifier,
) {
  val currentRoute by rootViewModel.currentRoute.collectAsState()
  val targetRoute = currentRoute

  if (targetRoute == null) {
    LoadingScreen(modifier)
  } else {
    NavDisplay(
      backStack = listOf(targetRoute),
      modifier = modifier,
      entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
      entryProvider = entryProvider {
        entry<RootRoute.Landing>(
          clazzContentKey = { "Landing" },
        ) {
          val landingViewModel: LandingViewModel = viewModel {
            dependencies.landingViewModelFactory.create()
          }
          LandingPage(landingViewModel)
        }
        entry<RootRoute.Main>(
          clazzContentKey = { mainRoute -> "Main:${mainRoute.serverUrl}" },
        ) { mainRoute ->
          val legacyMainAdapter: LegacyMainAdapterViewModel = viewModel(
            key = "LegacyMain:${mainRoute.serverUrl}"
          ) {
            val savedStateHandle = createSavedStateHandle()
            dependencies.legacyMainAdapterViewModelFactory.create(savedStateHandle)
          }
          LegacyMainPage(legacyMainAdapter)
        }
      }
    )
  }
}
