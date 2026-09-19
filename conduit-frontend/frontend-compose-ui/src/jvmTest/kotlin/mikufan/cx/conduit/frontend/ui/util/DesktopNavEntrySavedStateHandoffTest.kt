package mikufan.cx.conduit.frontend.ui.util

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private data class TestRoute(val id: String)

private class TestSavedStateViewModel(
  val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  var isCleared: Boolean = false
  override fun onCleared() {
    isCleared = true
  }
}

@OptIn(ExperimentalTestApi::class)
class DesktopNavEntrySavedStateHandoffTest {

  @Test
  fun testDesktopNavEntrySavedStateHandleHandoffAndTeardown() = runComposeUiTest {
    val route1 = TestRoute("route-1")
    val route2 = TestRoute("route-2")

    val backStackState = mutableStateOf(listOf(route1))
    val showRootState = mutableStateOf(true)
    val createdViewModels = mutableListOf<TestSavedStateViewModel>()

    setContent {
      if (showRootState.value) {
        DefaultRootCompositionLocalsProvider {
          NavDisplay(
            backStack = backStackState.value,
            modifier = Modifier.fillMaxSize(),
            entryDecorators = listOf(
              rememberSaveableStateHolderNavEntryDecorator(),
              rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
              entry<TestRoute>(
                clazzContentKey = { route -> "entry_${route.id}" },
              ) { route ->
                val vm: TestSavedStateViewModel = viewModel(key = "vm_${route.id}") {
                  val handle = createSavedStateHandle()
                  TestSavedStateViewModel(handle).also { createdViewModels += it }
                }
                Text("Content: ${route.id}, count=${vm.savedStateHandle.get<Int>("counter") ?: 0}")
              }
            },
          )
        }
      }
    }

    // 1. Initial display for route1
    onNodeWithText("Content: route-1, count=0").assertExists()
    assertEquals(1, createdViewModels.size)
    val vm1 = createdViewModels[0]
    assertFalse(vm1.isCleared)

    // Mutate SavedStateHandle
    vm1.savedStateHandle["counter"] = 42

    // 2. Push route2
    backStackState.value = listOf(route1, route2)
    waitForIdle()
    onNodeWithText("Content: route-2, count=0").assertExists()
    assertEquals(2, createdViewModels.size)
    val vm2 = createdViewModels[1]
    assertFalse(vm1.isCleared)
    assertFalse(vm2.isCleared)

    // 3. Pop back to route1
    backStackState.value = listOf(route1)
    waitForIdle()
    onNodeWithText("Content: route-1, count=42").assertExists()
    // vm2 was popped from stack, should be disposed
    assertTrue(vm2.isCleared)
    assertFalse(vm1.isCleared)
    assertSame(vm1, createdViewModels[0])

    // 4. Whole-tree teardown: remove root provider (simulating window close / app shutdown)
    showRootState.value = false
    waitForIdle()
    assertTrue(vm1.isCleared)
  }
}
