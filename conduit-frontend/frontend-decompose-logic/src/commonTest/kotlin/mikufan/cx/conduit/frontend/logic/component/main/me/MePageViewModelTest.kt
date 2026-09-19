package mikufan.cx.conduit.frontend.logic.component.main.me

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserInfo
import mikufan.cx.conduit.frontend.logic.service.main.MePageService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MePageViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val userConfigFlow = MutableStateFlow<UserConfigState>(UserConfigState.Landing)
  private lateinit var mePageService: MePageService
  private lateinit var storeFactory: MeStoreFactory
  private lateinit var viewModelFactory: MePageViewModelFactory
  private lateinit var viewModelStore: ViewModelStore
  private lateinit var testNavigator: TestNavigator

  private class TestNavigator : MeNavNavigator {
    val addArticleEvents = Channel<Unit>(Channel.UNLIMITED)
    val editProfileEvents = Channel<LoadedMe>(Channel.UNLIMITED)
    val popRouteEvents = Channel<String>(Channel.UNLIMITED)

    override fun onEditProfile(loadedMe: LoadedMe) {
      editProfileEvents.trySend(loadedMe)
    }

    override fun onAddArticle() {
      addArticleEvents.trySend(Unit)
    }

    override fun onPopRoute(entryId: String) {
      popRouteEvents.trySend(entryId)
    }
  }

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mePageService = mock()
    every { mePageService.userConfigFlow } returns userConfigFlow

    storeFactory = MeStoreFactory(DefaultStoreFactory(), mePageService, testDispatcher)
    viewModelFactory = MePageViewModelFactory(storeFactory)
    viewModelStore = ViewModelStore()
    testNavigator = TestNavigator()
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun testStoreDisposedOnViewModelClear() {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(MePageViewModel::class) {
          viewModelFactory.create(testNavigator)
        }
      }
    )
    val vm = provider[MePageViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testImmediateNavigationWithoutPreRunCurrent() = runTest(testDispatcher) {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(MePageViewModel::class) {
          viewModelFactory.create(testNavigator)
        }
      }
    )
    val vm = provider[MePageViewModel::class]

    // Immediately send intent without calling runCurrent first:
    // Synchronously installed labelsChannel ensures label is buffered without pre-runCurrent
    vm.send(MePageIntent.AddArticle)
    testScheduler.runCurrent()

    assertEquals(Unit, testNavigator.addArticleEvents.receive())
  }

  @Test
  fun testStateAndEditProfileNavigation() = runTest(testDispatcher) {
    val userInfo = UserInfo(email = "alice@example.com", username = "alice", bio = "hello bio", image = "avatar.png", token = "jwt")
    userConfigFlow.value = UserConfigState.OnLogin("https://conduit.example.com", userInfo)

    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(MePageViewModel::class) {
          viewModelFactory.create(testNavigator)
        }
      }
    )
    val vm = provider[MePageViewModel::class]
    testScheduler.runCurrent()

    assertTrue(vm.state.value is MePageState.Loaded)
    val loadedState = vm.state.value as MePageState.Loaded
    assertEquals("alice", loadedState.username)

    vm.send(MePageIntent.EditProfile)
    testScheduler.runCurrent()

    val passedLoadedMe = testNavigator.editProfileEvents.receive()
    assertEquals("alice", passedLoadedMe.username)
    assertEquals("hello bio", passedLoadedMe.bio)
  }
}
