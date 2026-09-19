package mikufan.cx.conduit.frontend.logic.component.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainNavViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var userConfigKStore: UserConfigKStore
  private lateinit var userConfigFlow: MutableStateFlow<UserConfigState>
  private lateinit var storeFactory: MainNavStoreFactory
  private lateinit var viewModelFactory: MainNavViewModelFactory
  private lateinit var viewModelStore: ViewModelStore

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    userConfigFlow = MutableStateFlow(UserConfigState.OnUrl("https://example.com"))
    userConfigKStore = mock()
    every { userConfigKStore.userConfigFlow } returns userConfigFlow

    storeFactory = MainNavStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      userConfigKStore,
      testDispatcher,
    )
    viewModelFactory = MainNavViewModelFactory(storeFactory)
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
        addInitializer(MainNavViewModel::class) {
          viewModelFactory.create(handle)
        }
      }
    )
    val vm = provider[MainNavViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testStateAndIntentForwarding() = runTest(testDispatcher) {
    val userInfo = UserInfo("alice@test.com", "alice", null, null, "token")
    userConfigFlow.value = UserConfigState.OnLogin("https://example.com", userInfo)

    val handle = SavedStateHandle()
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(MainNavViewModel::class) {
          viewModelFactory.create(handle)
        }
      }
    )
    val vm = provider[MainNavViewModel::class]
    testScheduler.runCurrent()

    assertTrue(vm.state.value.isReady)
    assertEquals(0, vm.state.value.pageIndex)

    vm.send(MainNavIntent.MenuIndexSwitching(2))
    testScheduler.runCurrent()

    assertEquals(2, vm.state.value.pageIndex)
    assertEquals(MainNavMenuItem.Me, vm.state.value.currentMenuItem)
  }
}
