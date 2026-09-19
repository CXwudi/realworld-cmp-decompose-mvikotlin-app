package mikufan.cx.conduit.frontend.logic.component.main

import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class MainNavStoreTest {
  private val testDispatcher = StandardTestDispatcher()
  private val userConfigStateChannel = Channel<UserConfigState>(Channel.UNLIMITED)

  private lateinit var userConfigKStore: UserConfigKStore
  private lateinit var mainNavStore: Store<MainNavIntent, MainNavState, Nothing>

  @BeforeTest
  fun setUp() {
    userConfigKStore = mock()
    every { userConfigKStore.userConfigFlow } returns userConfigStateChannel.receiveAsFlow()
    mainNavStore = MainNavStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      userConfigKStore,
      testDispatcher,
    ).createStore(autoInit = false)
  }

  @AfterTest
  fun tearDown() {
    mainNavStore.dispose()
  }

  @Test
  fun testBootstrapperOnLoginToLoggedIn() = runTest(testDispatcher) {
    val stateChannel = Channel<MainNavState>()

    // Given
    val userInfo = UserInfo(
      email = "test@example.com",
      username = "testuser",
      bio = "test bio",
      image = "test-image.png",
      token = "test-token"
    )

    // When
    val disposable = mainNavStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnLogin("test-url", userInfo))

    // Then - ignore initial state and get the updated state
    stateChannel.receive()
    val newState = stateChannel.receive()
    assertTrue(newState.isLoggedIn)
    assertEquals(3, newState.menuItems.size)
    assertTrue(newState.menuItems.contains(MainNavMenuItem.Feed))
    assertTrue(newState.menuItems.contains(MainNavMenuItem.Me))
    
    val favouriteItem = newState.menuItems.filterIsInstance<MainNavMenuItem.Favourite>().first()
    assertEquals("testuser", favouriteItem.username)

    disposable.dispose()
  }

  @Test
  fun testSwitchToNotLoggedInStateTransition() = runTest(testDispatcher) {
    val stateChannel = Channel<MainNavState>()

    // Given - start with logged in state
    val userInfo = UserInfo(
      email = "test@example.com",
      username = "testuser", 
      bio = null,
      image = null,
      token = "test-token"
    )
    
    val disposable = mainNavStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnLogin("test-url", userInfo))
    stateChannel.receive() // initial state
    stateChannel.receive() // logged in state

    // When - switch to OnUrl (not logged in)
    userConfigStateChannel.send(UserConfigState.OnUrl("test-url"))

    // Then - state should switch to not logged in
    val newState = stateChannel.receive()
    assertFalse(newState.isLoggedIn)
    assertEquals(2, newState.menuItems.size)
    assertTrue(newState.menuItems.contains(MainNavMenuItem.Feed))
    assertTrue(newState.menuItems.contains(MainNavMenuItem.SignInUp))

    disposable.dispose()
  }

  @Test
  fun testSwitchToLoggedInStateTransition() = runTest(testDispatcher) {
    val stateChannel = Channel<MainNavState>()

    // Given - start with not logged in state
    val disposable = mainNavStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnUrl("test-url"))
    stateChannel.receive() // initial state

    // When - switch to OnLogin (logged in)
    val userInfo = UserInfo(
      email = "newuser@example.com",
      username = "newuser",
      bio = "new bio", 
      image = "new-image.png",
      token = "new-token"
    )
    userConfigStateChannel.send(UserConfigState.OnLogin("test-url", userInfo))

    // Then - state should switch to logged in
    val newState = stateChannel.receive()
    assertTrue(newState.isLoggedIn)
    assertEquals(3, newState.menuItems.size)
    
    val favouriteItem = newState.menuItems.filterIsInstance<MainNavMenuItem.Favourite>().first()
    assertEquals("newuser", favouriteItem.username)

    disposable.dispose()
  }

  @Test
  fun testUsernameChangeForLoggedInUser() = runTest(testDispatcher) {
    val stateChannel = Channel<MainNavState>()

    // Given - start with logged in user
    val userInfo1 = UserInfo("test@example.com", "user1", null, null, "token1")
    
    val disposable = mainNavStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnLogin("test-url", userInfo1))
    stateChannel.receive() // initial state
    stateChannel.receive() // first logged in state

    // When - username changes
    val userInfo2 = UserInfo("test@example.com", "user2", null, null, "token2")
    userConfigStateChannel.send(UserConfigState.OnLogin("test-url", userInfo2))

    // Then - state should update with new username
    val newState = stateChannel.receive()
    assertTrue(newState.isLoggedIn)
    
    val favouriteItem = newState.menuItems.filterIsInstance<MainNavMenuItem.Favourite>().first()
    assertEquals("user2", favouriteItem.username)

    disposable.dispose()
  }

  @Test
  fun testMenuIndexSwitchingValidIndex() = runTest(testDispatcher) {
    // Given - not logged in state (2 menu items: Feed, SignInUp)
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnUrl("test-url"))

    // When - switch to index 1 (SignInUp)
    mainNavStore.accept(MainNavIntent.MenuIndexSwitching(1))

    // Then
    assertEquals(1, mainNavStore.state.pageIndex)
    assertEquals(MainNavMenuItem.SignInUp, mainNavStore.state.currentMenuItem)
  }

  @Test
  fun testMenuIndexSwitchingInvalidIndexTooHigh() = runTest(testDispatcher) {
    // Given - not logged in state (2 menu items)
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnUrl("test-url"))

    // When/Then - should throw IllegalArgumentException for index 2 (out of bounds)
    assertFailsWith<IllegalArgumentException> {
      mainNavStore.accept(MainNavIntent.MenuIndexSwitching(2))
    }
  }

  @Test
  fun testMenuIndexSwitchingInvalidIndexNegative() = runTest(testDispatcher) {
    // Given
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnUrl("test-url"))

    // When/Then - should throw IllegalArgumentException for negative index
    assertFailsWith<IllegalArgumentException> {
      mainNavStore.accept(MainNavIntent.MenuIndexSwitching(-1))
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun testMenuIndexSwitchingNoOpWhenSameIndex() = runTest(testDispatcher) {
    val stateChannel = Channel<MainNavState>()

    // Given
    val disposable = mainNavStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnUrl("test-url"))
    stateChannel.receive() // initial state

    // When - try to switch to same index
    mainNavStore.accept(MainNavIntent.MenuIndexSwitching(0))

    // Then - no state change should occur (channel should not receive anything new)
    assertTrue(stateChannel.isEmpty)
    assertEquals(0, mainNavStore.state.pageIndex)

    disposable.dispose()
  }

  @Test
  fun testMenuIndexSwitchingLoggedInState() = runTest(testDispatcher) {
    val stateChannel = Channel<MainNavState>()
    
    // Given - start with logged in state (3 menu items: Feed, Favourite, Me)
    val userInfo = UserInfo("test@example.com", "testuser", null, null, "token")
    val disposable = mainNavStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnLogin("test-url", userInfo))

    // Wait for state to transition to logged in
    stateChannel.receive() // initial state
    val loggedInState = stateChannel.receive() // logged in state
    assertTrue(loggedInState.isLoggedIn)
    assertEquals(3, loggedInState.menuItems.size)

    // When - switch to index 2 (Me)
    mainNavStore.accept(MainNavIntent.MenuIndexSwitching(2))

    // Then
    val switchedState = stateChannel.receive()
    assertEquals(2, switchedState.pageIndex)
    assertEquals(MainNavMenuItem.Me, switchedState.currentMenuItem)
    
    disposable.dispose()
  }


  @Test 
  fun testInitialStateNotLoggedIn() = runTest(testDispatcher) {
    // Test that initial state is correctly set to not logged in
    val initialState = mainNavStore.state
    assertFalse(initialState.isLoggedIn)
    assertEquals(0, initialState.pageIndex)
    assertEquals(2, initialState.menuItems.size)
    assertTrue(initialState.menuItems.contains(MainNavMenuItem.Feed))
    assertTrue(initialState.menuItems.contains(MainNavMenuItem.SignInUp))
  }

  @Test
  fun testLandingStateTransitionResetsToGuestState() = runTest(testDispatcher) {
    val stateChannel = Channel<MainNavState>()

    // Given - start with logged in state
    val userInfo = UserInfo(
      email = "test@example.com",
      username = "testuser",
      bio = null,
      image = null,
      token = "test-token"
    )

    val disposable = mainNavStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    mainNavStore.init()
    userConfigStateChannel.send(UserConfigState.OnLogin("test-url", userInfo))
    stateChannel.receive() // initial state
    val loggedIn = stateChannel.receive() // logged in state
    assertTrue(loggedIn.isLoggedIn)

    // When - KStore resets to Landing while MainNav is active (e.g. exit animation / race before disposal)
    userConfigStateChannel.send(UserConfigState.Landing)

    // Then - must be benign and reset to guest state, avoiding fatal exception
    val resetState = stateChannel.receive()
    assertFalse(resetState.isLoggedIn)
    assertEquals(2, resetState.menuItems.size)
    assertTrue(resetState.menuItems.contains(MainNavMenuItem.Feed))
    assertTrue(resetState.menuItems.contains(MainNavMenuItem.SignInUp))

    disposable.dispose()
  }
}