package mikufan.cx.conduit.frontend.logic.component.main.me

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MeNavViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var storeFactory: MeNavStoreFactory
  private lateinit var viewModelFactory: MeNavViewModelFactory
  private lateinit var viewModelStore: ViewModelStore

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    storeFactory = MeNavStoreFactory(DefaultStoreFactory(), testDispatcher)
    viewModelFactory = MeNavViewModelFactory(storeFactory)
    viewModelStore = ViewModelStore()
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun testStoreDisposedOnViewModelClear() {
    val handle = SavedStateHandle()
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(MeNavViewModel::class) {
          viewModelFactory.create(handle)
        }
      }
    )
    val vm = provider[MeNavViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testNavigationMethodsAndStateForwarding() = runTest(testDispatcher) {
    val handle = SavedStateHandle()
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(MeNavViewModel::class) {
          viewModelFactory.create(handle)
        }
      }
    )
    val vm = provider[MeNavViewModel::class]

    assertEquals(1, vm.state.value.stack.size)
    assertTrue(vm.state.value.currentRoute is MeNavRoute.Profile)

    val loadedMe = LoadedMe(email = "alice@example.com", username = "alice")
    vm.onEditProfile(loadedMe)
    testScheduler.runCurrent()

    assertEquals(2, vm.state.value.stack.size)
    val editRoute = vm.state.value.currentRoute as MeNavRoute.EditProfile
    assertEquals("edit_1", editRoute.entryId)
    assertEquals(loadedMe, editRoute.loadedMe)

    // Pop route via navigator callback
    vm.onPopRoute("edit_1")
    testScheduler.runCurrent()

    assertEquals(1, vm.state.value.stack.size)
    assertTrue(vm.state.value.currentRoute is MeNavRoute.Profile)

    // Navigate to Add Article
    vm.onAddArticle()
    testScheduler.runCurrent()

    assertEquals(2, vm.state.value.stack.size)
    assertEquals("add_2", vm.state.value.currentRoute.entryId)

    // Pop via back
    vm.pop()
    testScheduler.runCurrent()

    assertEquals(1, vm.state.value.stack.size)
    assertTrue(vm.state.value.currentRoute is MeNavRoute.Profile)
  }
}
