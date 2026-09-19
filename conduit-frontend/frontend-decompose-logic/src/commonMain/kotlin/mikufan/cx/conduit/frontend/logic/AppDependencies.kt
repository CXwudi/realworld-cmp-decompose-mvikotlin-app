package mikufan.cx.conduit.frontend.logic

import mikufan.cx.conduit.frontend.logic.component.landing.LandingViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.legacy.LegacyChildAdapterViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.legacy.LegacyMainAdapterViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.MainNavViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesPanelNavComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavComponentFactory
import mikufan.cx.conduit.frontend.logic.component.root.RootViewModelFactory

/**
 * Plain application dependencies resolved from DI container.
 * Free of any DI framework types, exposed to UI and platform entry points.
 */
class AppDependencies(
  val rootViewModelFactory: RootViewModelFactory,
  val landingViewModelFactory: LandingViewModelFactory,
  val mainNavViewModelFactory: MainNavViewModelFactory,
  val authViewModelFactory: AuthViewModelFactory,
  val legacyChildAdapterViewModelFactory: LegacyChildAdapterViewModelFactory,
  val articleListDetailComponentFactory: ArticlesPanelNavComponentFactory,
  val meNavComponentFactory: MeNavComponentFactory,
  val legacyMainAdapterViewModelFactory: LegacyMainAdapterViewModelFactory,
  val onShutdown: () -> Unit = {},
)
