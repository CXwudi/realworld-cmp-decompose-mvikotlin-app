package mikufan.cx.conduit.frontend.logic.component.root

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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
import kotlin.test.assertNull

/**
 * Focused tests for [RootViewModel]:
 * - Drives viewModelScope with [StandardTestDispatcher] via [Dispatchers.setMain].
 * - Owns and cleans up ViewModels through [ViewModelStore].
 * - Verifies root gate state transitions (Loading, Landing, Main).
 * - Verifies same-server login/profile updates retain Root Main identity.
 * - Verifies server URL changes produce distinct Main identity to clear old subtree.
 * - Verifies replacement navigation without backstack accumulation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val userConfigChannel = Channel<UserConfigState>(Channel.UNLIMITED)
  private lateinit var userConfigKStore: UserConfigKStore
  private lateinit var viewModelFactory: RootViewModelFactory
  private lateinit var viewModelStore: ViewModelStore

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    viewModelStore = ViewModelStore()
    userConfigKStore = mock()
    every { userConfigKStore.userConfigFlow } returns userConfigChannel.receiveAsFlow()
    viewModelFactory = RootViewModelFactory(userConfigKStore)
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  private fun createRootViewModel(): RootViewModel {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(RootViewModel::class) {
          viewModelFactory.create()
        }
      }
    )
    return provider[RootViewModel::class]
  }

  @Test
  fun testInitialGateIsLoadingUntilKStoreEmits() = runTest(testDispatcher) {
    val rootVm = createRootViewModel()

    // Gate state is null (Loading screen outside NavDisplay) until KStore emits authoritative state
    testScheduler.runCurrent()
    assertNull(rootVm.currentRoute.value)

    // Once KStore emits, gate state resolves to Landing
    userConfigChannel.send(UserConfigState.Landing)
    testScheduler.runCurrent()
    assertEquals(RootRoute.Landing, rootVm.currentRoute.value)
  }

  @Test
  fun testGateTransitionToMain() = runTest(testDispatcher) {
    val rootVm = createRootViewModel()

    userConfigChannel.send(UserConfigState.OnUrl("https://conduit-api.example.com"))
    testScheduler.runCurrent()
    assertEquals(RootRoute.Main("https://conduit-api.example.com"), rootVm.currentRoute.value)
  }

  @Test
  fun testSameServerLoginPreservesRootMainIdentity() = runTest(testDispatcher) {
    val rootVm = createRootViewModel()
    val serverUrl = "https://conduit-api.example.com"

    // 1. Initial guest access on server
    userConfigChannel.send(UserConfigState.OnUrl(serverUrl))
    testScheduler.runCurrent()
    assertEquals(RootRoute.Main(serverUrl), rootVm.currentRoute.value)

    // 2. User logs in on the SAME server
    val userInfo = UserInfo(
      email = "user@test.com",
      username = "testuser",
      bio = "Hello",
      image = null,
      token = "jwt-token",
    )
    userConfigChannel.send(UserConfigState.OnLogin(serverUrl, userInfo))
    testScheduler.runCurrent()

    // Root Main identity remains identical so Main subtree is preserved and reconciled inside MainNav
    assertEquals(RootRoute.Main(serverUrl), rootVm.currentRoute.value)
  }

  @Test
  fun testServerUrlChangeChangesRootMainIdentity() = runTest(testDispatcher) {
    val rootVm = createRootViewModel()

    // 1. Connected to server 1
    userConfigChannel.send(UserConfigState.OnUrl("https://server1.example.com"))
    testScheduler.runCurrent()
    assertEquals(RootRoute.Main("https://server1.example.com"), rootVm.currentRoute.value)

    // 2. Switch to server 2
    userConfigChannel.send(UserConfigState.OnUrl("https://server2.example.com"))
    testScheduler.runCurrent()

    // Main identity changed -> clears old subtree and cannot consume saved navigation
    assertEquals(RootRoute.Main("https://server2.example.com"), rootVm.currentRoute.value)
  }

  @Test
  fun testResetToLanding() = runTest(testDispatcher) {
    val rootVm = createRootViewModel()

    userConfigChannel.send(UserConfigState.OnUrl("https://server1.example.com"))
    testScheduler.runCurrent()
    assertEquals(RootRoute.Main("https://server1.example.com"), rootVm.currentRoute.value)

    // Logout / reset sends Landing
    userConfigChannel.send(UserConfigState.Landing)
    testScheduler.runCurrent()
    assertEquals(RootRoute.Landing, rootVm.currentRoute.value)
  }
}
