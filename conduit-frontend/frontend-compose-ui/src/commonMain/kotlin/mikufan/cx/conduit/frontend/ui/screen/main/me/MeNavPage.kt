package mikufan.cx.conduit.frontend.ui.screen.main.me

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import mikufan.cx.conduit.frontend.logic.AppDependencies
import mikufan.cx.conduit.frontend.logic.component.main.me.AddArticleViewModel
import mikufan.cx.conduit.frontend.logic.component.main.me.EditProfileViewModel
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavComponent
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavComponentChild
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavRoute
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavViewModel
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageViewModel

/**
 * Native Me navigation page displaying a [NavDisplay] stack owned by [MeNavViewModel].
 */
@Composable
fun MeNavPage(
  meNavViewModel: MeNavViewModel,
  dependencies: AppDependencies,
  modifier: Modifier = Modifier,
) {
  val meNavState by meNavViewModel.state.collectAsState()

  NavDisplay(
    backStack = meNavState.stack,
    modifier = modifier,
    onBack = meNavViewModel::pop,
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
      entry<MeNavRoute.Profile>(
        clazzContentKey = { profileRoute -> profileRoute.entryId },
      ) { profileRoute ->
        val vm: MePageViewModel = viewModel(
          key = "MePage_${profileRoute.entryId}",
        ) {
          dependencies.mePageViewModelFactory.create(
            navigator = meNavViewModel,
          )
        }
        MePage(vm, modifier = Modifier.fillMaxSize())
      }

      entry<MeNavRoute.EditProfile>(
        clazzContentKey = { editRoute -> editRoute.entryId },
      ) { editRoute ->
        val vm: EditProfileViewModel = viewModel(
          key = "EditProfile_${editRoute.entryId}",
        ) {
          dependencies.editProfileViewModelFactory.create(
            loadedMe = editRoute.loadedMe,
            entryId = editRoute.entryId,
            navigator = meNavViewModel,
          )
        }
        EditProfilePage(vm, modifier = Modifier.fillMaxSize())
      }

      entry<MeNavRoute.AddArticle>(
        clazzContentKey = { addRoute -> addRoute.entryId },
      ) { addRoute ->
        val vm: AddArticleViewModel = viewModel(
          key = "AddArticle_${addRoute.entryId}",
        ) {
          dependencies.addArticleViewModelFactory.create(
            entryId = addRoute.entryId,
            navigator = meNavViewModel,
          )
        }
        AddArticlePage(vm, modifier = Modifier.fillMaxSize())
      }
    }
  )
}

/**
 * Legacy Decompose overload preserved for previews or existing component tests.
 */
@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun MeNavPage(meNavComponent: MeNavComponent, modifier: Modifier = Modifier) {
  ChildStack(
    stack = meNavComponent.childStack,
    modifier = modifier,
    animation = stackAnimation(fade() + scale())
  ) {
    when (val child = it.instance) {
      is MeNavComponentChild.MePage -> MePage(child.mePageComponent)
      is MeNavComponentChild.EditProfile -> EditProfilePage(child.editProfileComponent)
      is MeNavComponentChild.AddArticle -> AddArticlePage(child.addArticleComponent)
    }
  }
}
