package mikufan.cx.conduit.frontend.logic.component.main.me

import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.ExperimentalMviKotlinApi
import com.arkivanov.mvikotlin.extensions.coroutines.labelsChannel
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mikufan.cx.conduit.frontend.logic.service.main.MePageService
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserInfo
import kotlinx.coroutines.flow.flowOf
import dev.mokkery.every
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeStoreTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var mePageService: MePageService
  private lateinit var meStore: Store<MePageIntent, MePageState, MePageLabel>

  @BeforeTest
  fun setUp() {
    mePageService = mock()
    meStore = MeStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      mePageService,
      testDispatcher,
    ).createStore(autoInit = false)
  }

  @AfterTest
  fun reset() {
    meStore.dispose()
  }

  @Test
  fun testLoadMeSuccess() = runTest(testDispatcher) {
    val stateChannel = Channel<MePageState>()

    // Given
    val loadedMe = LoadedMe(
      email = "test@email.com",
      username = "testuser",
      bio = "test bio",
      imageUrl = "test image url"
    )

    assertTrue { meStore.state is MePageState.Loading }

    // When
    val disposable = meStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    val userInfo = UserInfo(
      email = loadedMe.email,
      username = loadedMe.username,
      bio = loadedMe.bio,
      image = loadedMe.imageUrl,
      token = "test-token"
    )
    every { mePageService.userConfigFlow } returns flowOf(UserConfigState.OnLogin("test-url", userInfo))
    meStore.init()

    // Then
    stateChannel.receive() // ignore the first initial state
    val newState = stateChannel.receive()
    assertTrue { newState is MePageState.Loaded }
    (newState as MePageState.Loaded).let {
      assertEquals(loadedMe.email, it.email)
      assertEquals(loadedMe.username, it.username)
      assertEquals(loadedMe.bio, it.bio)
      assertEquals(loadedMe.imageUrl, it.imageUrl)
    }

    disposable.dispose()
  }

  @Test
  fun testLoadMeError() = runTest(testDispatcher) {
    val stateChannel = Channel<MePageState>()

    // Given
    val errorMessage = "Failed to load current user"
    assertTrue { meStore.state is MePageState.Loading }

    // When
    val disposable = meStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    every { mePageService.userConfigFlow } returns flowOf(UserConfigState.Landing)
    meStore.init()

    // Then
    stateChannel.receive() // ignore the first initial state
    val newState = stateChannel.receive()
    assertTrue { newState is MePageState.Error }
    assertEquals("User not logged in", (newState as MePageState.Error).errorMsg)

    disposable.dispose()
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testLogout() = runTest(testDispatcher) {
    val stateChannel = Channel<MePageState>()
    val testScope = TestScope(testDispatcher)
    val labelChannel = meStore.labelsChannel(testScope)

    // Given
    val loadedMe = LoadedMe(
      email = "test@email.com",
      username = "testuser",
      bio = "test bio",
      imageUrl = "test image url"
    )

    // When
    val disposable = meStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    val userInfo = UserInfo(
      email = loadedMe.email,
      username = loadedMe.username,
      bio = loadedMe.bio,
      image = loadedMe.imageUrl,
      token = "test-token"
    )
    every { mePageService.userConfigFlow } returns flowOf(UserConfigState.OnLogin("test-url", userInfo))
    meStore.init()

    // And
    stateChannel.receive() // ignore the first initial state
    stateChannel.receive() // wait until the user info is loaded
    meStore.accept(MePageIntent.Logout) // simulate user action

    // Then
    assertEquals(MePageLabel.TestOnly, labelChannel.receive())
    verifySuspend(exactly(1)) { mePageService.logout() }

    // And
    disposable.dispose()
    testScope.cancel()
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testSwitchServer() = runTest(testDispatcher) {
    val stateChannel = Channel<MePageState>()
    val testScope = TestScope(testDispatcher)
    val labelChannel = meStore.labelsChannel(testScope)

    // Given
    val loadedMe = LoadedMe(
      email = "test@email.com",
      username = "testuser",
      bio = "test bio",
      imageUrl = "test image url"
    )

    // When
    val disposable = meStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    val userInfo = UserInfo(
      email = loadedMe.email,
      username = loadedMe.username,
      bio = loadedMe.bio,
      image = loadedMe.imageUrl,
      token = "test-token"
    )
    every { mePageService.userConfigFlow } returns flowOf(UserConfigState.OnLogin("test-url", userInfo))
    meStore.init()

    // And
    stateChannel.receive() // ignore the first initial state
    stateChannel.receive() // wait until the user info is loaded
    meStore.accept(MePageIntent.SwitchServer) // simulate user action

    // Then
    assertEquals(MePageLabel.TestOnly, labelChannel.receive())
    verifySuspend(exactly(1)) { mePageService.switchServer() }

    // And
    disposable.dispose()
    testScope.cancel()
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testAddArticle() = runTest(testDispatcher) {
    val stateChannel = Channel<MePageState>()
    val testScope = TestScope(testDispatcher)
    val labelChannel = meStore.labelsChannel(testScope)

    // Given
    val loadedMe = LoadedMe(
      email = "test@email.com",
      username = "testuser",
      bio = "test bio",
      imageUrl = "test image url"
    )
    // When
    val disposable = meStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    val userInfo = UserInfo(
      email = loadedMe.email,
      username = loadedMe.username,
      bio = loadedMe.bio,
      image = loadedMe.imageUrl,
      token = "test-token"
    )
    every { mePageService.userConfigFlow } returns flowOf(UserConfigState.OnLogin("test-url", userInfo))
    meStore.init()

    // And
    stateChannel.receive() // ignore the first initial state
    stateChannel.receive() // wait until the user info is loaded
    meStore.accept(MePageIntent.AddArticle) // simulate user action

    // Then
    assertEquals(MePageLabel.AddArticle, labelChannel.receive())

    // And
    disposable.dispose()
    testScope.cancel()
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testEditProfile() = runTest(testDispatcher) {
    val stateChannel = Channel<MePageState>()
    val testScope = TestScope(testDispatcher)
    val labelChannel = meStore.labelsChannel(testScope)

    // Given
    val loadedMe = LoadedMe(
      email = "test@email.com",
      username = "testuser",
      bio = "test bio",
      imageUrl = "test image url"
    )
    // When
    val disposable = meStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    val userInfo = UserInfo(
      email = loadedMe.email,
      username = loadedMe.username,
      bio = loadedMe.bio,
      image = loadedMe.imageUrl,
      token = "test-token"
    )
    every { mePageService.userConfigFlow } returns flowOf(UserConfigState.OnLogin("test-url", userInfo))
    meStore.init()

    // And
    stateChannel.receive() // ignore the first initial state
    stateChannel.receive() // wait until the user info is loaded
    meStore.accept(MePageIntent.EditProfile) // simulate user action

    // Then
    val receivedLabel = labelChannel.receive()
    assertTrue { receivedLabel is MePageLabel.EditProfile }
    val editProfileLabel = receivedLabel as MePageLabel.EditProfile
    assertEquals(loadedMe, editProfileLabel.loadedMe)

    // And
    disposable.dispose()
    testScope.cancel()
  }
}
