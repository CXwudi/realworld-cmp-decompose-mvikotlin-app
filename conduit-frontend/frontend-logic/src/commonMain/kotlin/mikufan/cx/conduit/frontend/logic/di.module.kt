package mikufan.cx.conduit.frontend.logic

import mikufan.cx.conduit.frontend.logic.component.viewModelModules
import mikufan.cx.conduit.frontend.logic.repo.repoModules
import mikufan.cx.conduit.frontend.logic.service.serviceModule
import org.koin.core.Koin

/**
 * All modules required for setting up the Koin DI.
 */
val allModules =
  // repo layer
  repoModules +
  // service layer (business logic)
  listOf(serviceModule) +
  // component/viewModel layer (multiplatform navigation + MVI)
  viewModelModules

/**
 * Resolves plain [AppDependencies] from the DI container.
 * Encapsulates all Koin resolution within DI files.
 */
fun createAppDependencies(
  koin: Koin,
  onShutdown: () -> Unit = { koin.close() },
): AppDependencies = AppDependencies(
  rootViewModelFactory = koin.get(),
  landingViewModelFactory = koin.get(),
  mainNavViewModelFactory = koin.get(),
  authViewModelFactory = koin.get(),
  articlesNavViewModelFactory = koin.get(),
  articlesListViewModelFactory = koin.get(),
  articleDetailViewModelFactory = koin.get(),
  meNavViewModelFactory = koin.get(),
  mePageViewModelFactory = koin.get(),
  editProfileViewModelFactory = koin.get(),
  addArticleViewModelFactory = koin.get(),
  onShutdown = onShutdown,
)
