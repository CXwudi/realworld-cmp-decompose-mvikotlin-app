package mikufan.cx.conduit.frontend.logic.component.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.statekeeper.ExperimentalStateKeeperApi
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.extensions.coroutines.states
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageComponent
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListDetailNavComponent
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesPanelNavComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesSearchFilter
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavComponent
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavComponentFactory
import mikufan.cx.conduit.frontend.logic.component.util.MviComponent

/**
 * The component for the main page, it unfortunately breaks the MVI pattern where it has two state values.
 *
 * One from the Decompose navigation ([childStack]) and another we create ourselves ([MainNavState]).
 *
 */
interface MainNavComponent : MviComponent<MainNavIntent, MainNavState> {
  val childStack: Value<ChildStack<*, MainNavComponentChild>>
}

sealed interface MainNavComponentChild {
  data class MainFeed(val component: ArticlesListDetailNavComponent) : MainNavComponentChild
  data class Favourite(val component: ArticlesListDetailNavComponent) : MainNavComponentChild
  data class Me(val component: MeNavComponent) : MainNavComponentChild
  data class SignInUp(val component: AuthPageComponent) : MainNavComponentChild
}

class DefaultMainNavComponent(
  componentContext: ComponentContext,
  private val mainNavStoreFactory: MainNavStoreFactory,
  private val authPageComponentFactory: AuthPageComponentFactory,
  private val meNavComponentFactory: MeNavComponentFactory,
  val articleListDetailComponentFactory: ArticlesPanelNavComponentFactory,
) : MainNavComponent, ComponentContext by componentContext {

  @OptIn(ExperimentalStateKeeperApi::class)
  private val store = instanceKeeper.getStore {
    val state = stateKeeper.consume("MainNavState", MainNavState.serializer()) ?: MainNavState.notLoggedIn()
    mainNavStoreFactory.createStore(initState = state)
  }

  override val state: StateFlow<MainNavState> = store.stateFlow(coroutineScope())

  override fun send(intent: MainNavIntent) = store.accept(intent)

  private val stackNavigation = StackNavigation<Config>()

  override val childStack: Value<ChildStack<*, MainNavComponentChild>> =
    childStack(
      source = stackNavigation,
      initialConfiguration = enumToConfig(store.state.currentMenuItem),
      serializer = null,
      childFactory = ::childFactory
    )

  init {
    coroutineScope().launch {
      setupUserConfigStateToNavigationMapping()
    }
    stateKeeper.register("MainNavState", MainNavState.serializer()) { state.value }
  }

  private suspend fun setupUserConfigStateToNavigationMapping() {
    store.states
      .collectLatest {
        log.debug { "Switching to $it" }
        stackNavigation.replaceCurrent(enumToConfig(it.currentMenuItem))
      }
  }

  private fun childFactory(
    config: Config,
    componentContext: ComponentContext
  ): MainNavComponentChild = when (config) {
    Config.MainFeed -> MainNavComponentChild.MainFeed(
      articleListDetailComponentFactory.create(componentContext)
    )
    is Config.Favourite -> {
      val searchFilter = ArticlesSearchFilter(favoritedByUsername = config.username)
      MainNavComponentChild.Favourite(
        articleListDetailComponentFactory.create(componentContext, searchFilter)
      )
    }
    Config.Me -> MainNavComponentChild.Me(
      meNavComponentFactory.create(componentContext)
    )
    Config.SignInUp -> MainNavComponentChild.SignInUp(
      authPageComponentFactory.create(componentContext)
    )
  }

  private fun enumToConfig(enum: MainNavMenuItem): Config = when (enum) {
    MainNavMenuItem.Feed -> Config.MainFeed
    is MainNavMenuItem.Favourite -> Config.Favourite(enum.username)
    MainNavMenuItem.Me -> Config.Me
    MainNavMenuItem.SignInUp -> Config.SignInUp
  }

  @Serializable
  sealed interface Config {

    @Serializable
    data object MainFeed : Config

    @Serializable
    data class Favourite(val username: String) : Config

    @Serializable
    data object Me : Config

    @Serializable
    data object SignInUp : Config
  }
}

class MainNavComponentFactory(
  private val storeFactory: MainNavStoreFactory,
  private val articleListDetailComponentFactory: ArticlesPanelNavComponentFactory,
  private val authPageComponentFactory: AuthPageComponentFactory,
  private val meNavComponentFactory: MeNavComponentFactory,
) {
  fun create(componentContext: ComponentContext) = DefaultMainNavComponent(
    componentContext = componentContext,
    mainNavStoreFactory = storeFactory,
    articleListDetailComponentFactory = articleListDetailComponentFactory,
    authPageComponentFactory = authPageComponentFactory,
    meNavComponentFactory = meNavComponentFactory,
  )
}

private val log = KotlinLogging.logger { }