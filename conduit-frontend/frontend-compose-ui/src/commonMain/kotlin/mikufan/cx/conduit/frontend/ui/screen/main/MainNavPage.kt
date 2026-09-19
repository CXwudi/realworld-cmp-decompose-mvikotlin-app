package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import mikufan.cx.conduit.frontend.logic.AppDependencies
import mikufan.cx.conduit.frontend.logic.component.legacy.LegacyChildAdapterViewModel
import mikufan.cx.conduit.frontend.logic.component.main.MainChildRoute
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponent
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponentChild
import mikufan.cx.conduit.frontend.logic.component.main.MainNavIntent
import mikufan.cx.conduit.frontend.logic.component.main.MainNavMenuItem
import mikufan.cx.conduit.frontend.logic.component.main.MainNavViewModel
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthViewModel
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListDetailNavComponent
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesSearchFilter
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavComponent
import mikufan.cx.conduit.frontend.ui.screen.main.auth.AuthPage
import mikufan.cx.conduit.frontend.ui.screen.main.feed.ArticlesListDetailPanel
import mikufan.cx.conduit.frontend.ui.screen.main.me.MeNavPage

/**
 * Native CMP Main navigation page hosting tab switching and entry-owned child routes.
 */
@Composable
fun MainNavPage(
  viewModel: MainNavViewModel,
  dependencies: AppDependencies,
  modifier: Modifier = Modifier,
) {
  val mainNavState by viewModel.state.collectAsState()

  val selectedIndex by remember { derivedStateOf { mainNavState.pageIndex } }
  val menuItems = remember { derivedStateOf { mainNavState.menuItems } }

  val navItems = remember(menuItems.value) {
    navigationItems(menuItems.value, viewModel::send)
  }

  val currentChildRoute = mainNavState.currentChildRoute

  NavigationSuiteScaffold(
    navigationSuiteItems = {
      navItems.forEach { item ->
        item(
          icon = { Icon(item.icon, contentDescription = item.label) },
          label = { Text(item.label) },
          selected = selectedIndex == item.index,
          onClick = item.onClick,
        )
      }
    },
    modifier = modifier,
  ) {
    if (currentChildRoute == null) {
      Box(modifier = Modifier.fillMaxSize())
    } else {
      NavDisplay(
        backStack = listOf(currentChildRoute),
        modifier = Modifier.fillMaxSize(),
        entryDecorators = listOf(
          rememberSaveableStateHolderNavEntryDecorator(),
          rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
          entry<MainChildRoute.Feed>(
            clazzContentKey = { feedRoute -> "Feed_${feedRoute.generation}" },
          ) { feedRoute ->
            val adapter: LegacyChildAdapterViewModel<ArticlesListDetailNavComponent> = viewModel(
              key = "LegacyFeed_${feedRoute.generation}",
            ) {
              val savedStateHandle = createSavedStateHandle()
              dependencies.legacyChildAdapterViewModelFactory.create(
                savedStateHandle = savedStateHandle,
                saveKey = "legacy_feed",
              ) { ctx ->
                dependencies.articleListDetailComponentFactory.create(ctx)
              }
            }
            LegacySubtreeHost(adapter) { child ->
              ArticlesListDetailPanel(child)
            }
          }

          entry<MainChildRoute.Favourite>(
            clazzContentKey = { favRoute -> "Favourite_${favRoute.username}_${favRoute.generation}" },
          ) { favRoute ->
            val adapter: LegacyChildAdapterViewModel<ArticlesListDetailNavComponent> = viewModel(
              key = "LegacyFav_${favRoute.username}_${favRoute.generation}",
            ) {
              val savedStateHandle = createSavedStateHandle()
              val searchFilter = ArticlesSearchFilter(favoritedByUsername = favRoute.username)
              dependencies.legacyChildAdapterViewModelFactory.create(
                savedStateHandle = savedStateHandle,
                saveKey = "legacy_fav_${favRoute.username}",
              ) { ctx ->
                dependencies.articleListDetailComponentFactory.create(ctx, searchFilter)
              }
            }
            LegacySubtreeHost(adapter) { child ->
              ArticlesListDetailPanel(child)
            }
          }

          entry<MainChildRoute.Me>(
            clazzContentKey = { meRoute -> "Me_${meRoute.generation}" },
          ) { meRoute ->
            val adapter: LegacyChildAdapterViewModel<MeNavComponent> = viewModel(
              key = "LegacyMe_${meRoute.generation}",
            ) {
              val savedStateHandle = createSavedStateHandle()
              dependencies.legacyChildAdapterViewModelFactory.create(
                savedStateHandle = savedStateHandle,
                saveKey = "legacy_me",
              ) { ctx ->
                dependencies.meNavComponentFactory.create(ctx)
              }
            }
            LegacySubtreeHost(adapter) { child ->
              MeNavPage(child)
            }
          }

          entry<MainChildRoute.Auth>(
            clazzContentKey = { authRoute -> "Auth_${authRoute.generation}" },
          ) { authRoute ->
            val authViewModel: AuthViewModel = viewModel(
              key = "Auth_${authRoute.generation}",
            ) {
              dependencies.authViewModelFactory.create()
            }
            AuthPage(authViewModel)
          }
        },
      )
    }
  }
}

/**
 * Legacy Decompose component overload retained for previews and backwards compatibility.
 */
@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun MainNavPage(component: MainNavComponent, modifier: Modifier = Modifier) {
  val mainNavState by component.state.collectAsState()
  val stack by component.childStack.subscribeAsState()

  val selectedIndex by remember { derivedStateOf { mainNavState.pageIndex } }
  val menuItems = remember { derivedStateOf { mainNavState.menuItems } }

  val navItems = remember(menuItems.value) {
    navigationItems(menuItems.value, component::send)
  }

  NavigationSuiteScaffold(
    navigationSuiteItems = {
      navItems.forEach { item ->
        item(
          icon = { Icon(item.icon, contentDescription = item.label) },
          label = { Text(item.label) },
          selected = selectedIndex == item.index,
          onClick = item.onClick,
        )
      }
    },
    modifier = modifier,
  ) {
    ChildStack(
      stack = component.childStack,
      animation = stackAnimation(),
    ) {
      when (val child = it.instance) {
        is MainNavComponentChild.MainFeed -> ArticlesListDetailPanel(child.component)
        is MainNavComponentChild.Favourite -> ArticlesListDetailPanel(child.component)
        is MainNavComponentChild.Me -> MeNavPage(child.component)
        is MainNavComponentChild.SignInUp -> AuthPage(child.component)
      }
    }
  }
}

private fun navigationItems(
  menuItems: List<MainNavMenuItem>,
  onSend: (MainNavIntent) -> Unit,
): List<NavigationItem> {
  return menuItems.withIndex().map { (index, value) ->
    mapMenuItem2NavItem(value, index, onSend)
  }
}

data class NavigationItem(
  val label: String,
  val icon: ImageVector,
  val index: Int,
  val onClick: () -> Unit,
)

private fun mapMenuItem2NavItem(
  value: MainNavMenuItem,
  index: Int,
  onSend: (MainNavIntent) -> Unit,
): NavigationItem {
  val icon = when (value) {
    MainNavMenuItem.Feed -> Icons.Filled.Home
    is MainNavMenuItem.Favourite -> Icons.Filled.Favorite
    MainNavMenuItem.Me -> Icons.Filled.Person
    MainNavMenuItem.SignInUp -> Icons.Filled.Person
  }

  val item = NavigationItem(value.menuName, icon, index) {
    onSend(MainNavIntent.MenuIndexSwitching(index))
  }

  return item
}
