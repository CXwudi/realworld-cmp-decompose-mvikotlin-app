package mikufan.cx.conduit.frontend.ui.screen.main.feed

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleBasicInfo
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesNavRoute
import mikufan.cx.conduit.frontend.ui.util.DefaultRootCompositionLocalsProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestListViewModel : ViewModel() {
  var isCleared: Boolean = false
  override fun onCleared() {
    isCleared = true
  }
}

class TestDetailViewModel(val entryId: String) : ViewModel() {
  var isCleared: Boolean = false
  override fun onCleared() {
    isCleared = true
  }
}

@OptIn(ExperimentalTestApi::class)
class ArticlesNavDisplayOwnershipTest {

  @Test
  fun testNavDisplayDecoratorsAndResizeOwnership() = runComposeUiTest {
    val listRoute = ArticlesNavRoute.List("list")
    val detailRoute1 = ArticlesNavRoute.Detail(
      basicInfo = ArticleBasicInfo(null, "alice", "Article 1", "slug-1"),
      entryId = "detail_1",
    )
    val detailRoute2 = ArticlesNavRoute.Detail(
      basicInfo = ArticleBasicInfo(null, "bob", "Article 2", "slug-2"),
      entryId = "detail_2",
    )

    val backStackState = mutableStateOf<List<ArticlesNavRoute>>(listOf(listRoute, detailRoute1))
    val isDualPaneState = mutableStateOf(false) // Start in compact mode
    val showArticlesNav = mutableStateOf(true)

    val createdListViewModels = mutableListOf<TestListViewModel>()
    val createdDetailViewModels = mutableListOf<TestDetailViewModel>()

    setContent {
      DefaultRootCompositionLocalsProvider {
        if (showArticlesNav.value) {
          val isDualPane = isDualPaneState.value
          val sceneStrategy = ArticlesTwoPaneSceneStrategy<ArticlesNavRoute>(isDualPane)

          NavDisplay(
            backStack = backStackState.value,
            modifier = Modifier.fillMaxSize(),
            sceneStrategies = listOf(sceneStrategy),
            entryDecorators = listOf(
              rememberSaveableStateHolderNavEntryDecorator(),
              rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
              entry<ArticlesNavRoute.List>(
                clazzContentKey = { route -> "List_${route.entryId}" },
              ) { route ->
                val vm: TestListViewModel = viewModel(key = "List_${route.entryId}") {
                  TestListViewModel().also { createdListViewModels += it }
                }
                Text("List Content [${route.entryId}]")
              }

              entry<ArticlesNavRoute.Detail>(
                clazzContentKey = { route -> "Detail_${route.entryId}" },
              ) { route ->
                val vm: TestDetailViewModel = viewModel(key = "Detail_${route.entryId}") {
                  TestDetailViewModel(route.entryId).also { createdDetailViewModels += it }
                }
                Text("Detail Content [${route.basicInfo.title}]")
              }
            },
          )
        }
      }
    }

    // 1. In compact mode with [List, Detail 1], single-pane fallback displays Detail 1
    onNodeWithText("Detail Content [Article 1]").assertExists()
    assertEquals(1, createdDetailViewModels.size, "Detail 1 must be created exactly once initially")
    val detail1Vm = createdDetailViewModels[0]
    assertEquals("detail_1", detail1Vm.entryId)
    assertFalse(detail1Vm.isCleared)

    // 2. Resize to Wide / DualPane (isDualPane = true)
    isDualPaneState.value = true
    waitForIdle()

    // Both List and Detail 1 are now displayed in dual-pane
    onNodeWithText("List Content [list]").assertExists()
    onNodeWithText("Detail Content [Article 1]").assertExists()

    assertEquals(1, createdListViewModels.size, "List ViewModel must be created exactly once")
    val listVm = createdListViewModels[0]
    assertFalse(listVm.isCleared)

    assertEquals(1, createdDetailViewModels.size, "Detail 1 must not be recreated when resizing to dual-pane")
    assertSame(detail1Vm, createdDetailViewModels[0], "Detail 1 instance must be preserved across resize")
    assertFalse(detail1Vm.isCleared)

    // 3. User selects another article (Detail 1 replaced with Detail 2, preserving List)
    backStackState.value = listOf(listRoute, detailRoute2)
    waitForIdle()

    onNodeWithText("List Content [list]").assertExists()
    onNodeWithText("Detail Content [Article 2]").assertExists()

    // List VM must NOT be recreated
    assertEquals(1, createdListViewModels.size, "List ViewModel must not be recreated when detail replaced")
    assertSame(listVm, createdListViewModels[0])
    assertFalse(listVm.isCleared)

    // Old Detail 1 ViewModel must be cleared upon removal from backStack!
    assertTrue(detail1Vm.isCleared, "Replaced detail 1 ViewModel must be cleared upon removal from backStack")

    // New Detail 2 ViewModel is instantiated and alive
    assertEquals(2, createdDetailViewModels.size, "Detail 2 ViewModel must be instantiated")
    val detail2Vm = createdDetailViewModels.first { it.entryId == "detail_2" }
    assertFalse(detail2Vm.isCleared)

    // 4. Resize back to Compact (isDualPane = false)
    isDualPaneState.value = false
    waitForIdle()

    onNodeWithText("Detail Content [Article 2]").assertExists()
    assertEquals(1, createdListViewModels.size, "List ViewModel must survive resizing back to compact")
    assertSame(listVm, createdListViewModels[0])
    assertFalse(listVm.isCleared, "List ViewModel must stay alive while underneath in compact")

    assertEquals(2, createdDetailViewModels.size, "Detail 2 ViewModel must not be recreated when resizing to compact")
    assertSame(detail2Vm, createdDetailViewModels.first { it.entryId == "detail_2" })
    assertFalse(detail2Vm.isCleared)

    // 5. User pops detail (backStack becomes [List])
    backStackState.value = listOf(listRoute)
    waitForIdle()

    onNodeWithText("List Content [list]").assertExists()
    assertTrue(detail2Vm.isCleared, "Closed detail 2 ViewModel must be cleared upon pop")
    assertEquals(1, createdListViewModels.size)
    assertSame(listVm, createdListViewModels[0])
    assertFalse(listVm.isCleared, "List ViewModel must remain alive on root list")

    // 6. Truly remove the owning tree (e.g. tab switch away in MainNav)
    showArticlesNav.value = false
    waitForIdle()

    assertTrue(listVm.isCleared, "Root list ViewModel must be cleared when owning tree is removed")
  }
}
