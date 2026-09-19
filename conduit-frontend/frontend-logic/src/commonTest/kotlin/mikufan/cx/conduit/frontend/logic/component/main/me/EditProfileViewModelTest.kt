package mikufan.cx.conduit.frontend.logic.component.main.me

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.calls
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mikufan.cx.conduit.frontend.logic.service.main.EditProfileService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var editProfileService: EditProfileService
  private lateinit var storeFactory: EditProfileStoreFactory
  private lateinit var viewModelFactory: EditProfileViewModelFactory
  private lateinit var viewModelStore: ViewModelStore
  private lateinit var testNavigator: TestNavigator

  private val initialLoadedMe = LoadedMe(
    email = "alice@example.com",
    username = "alice",
    bio = "initial bio",
    imageUrl = "initial.png",
  )

  private class TestNavigator : MeNavNavigator {
    val popRouteEvents = Channel<String>(Channel.UNLIMITED)

    override fun onEditProfile(loadedMe: LoadedMe) {}
    override fun onAddArticle() {}

    override fun onPopRoute(entryId: String) {
      popRouteEvents.trySend(entryId)
    }
  }

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    editProfileService = mock()
    storeFactory = EditProfileStoreFactory(DefaultStoreFactory(), editProfileService, testDispatcher)
    viewModelFactory = EditProfileViewModelFactory(storeFactory)
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
        addInitializer(EditProfileViewModel::class) {
          viewModelFactory.create(initialLoadedMe, "edit_1", testNavigator)
        }
      }
    )
    val vm = provider[EditProfileViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testImmediateBackWithoutPreRunCurrent() = runTest(testDispatcher) {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(EditProfileViewModel::class) {
          viewModelFactory.create(initialLoadedMe, "edit_1", testNavigator)
        }
      }
    )
    val vm = provider[EditProfileViewModel::class]

    // Immediately send intent without calling runCurrent first:
    // Synchronously installed labelsChannel ensures label is buffered without pre-runCurrent
    vm.send(EditProfileIntent.BackWithoutSave)
    testScheduler.runCurrent()

    assertEquals("edit_1", testNavigator.popRouteEvents.receive())
  }

  @Test
  fun testDeferredSaveNavigatesWithoutUiCollector() = runTest(testDispatcher) {
    val requestStarted = CompletableDeferred<Unit>()
    val responseRelease = CompletableDeferred<Unit>()
    everySuspend { editProfileService.updateAndSave(any()) } calls {
      requestStarted.complete(Unit)
      responseRelease.await()
    }

    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(EditProfileViewModel::class) {
          viewModelFactory.create(initialLoadedMe, "edit_1", testNavigator)
        }
      }
    )
    val vm = provider[EditProfileViewModel::class]

    // Update bio and save
    vm.send(EditProfileIntent.BioChanged("updated bio"))
    vm.send(EditProfileIntent.Save)

    // Await requestStarted: ensures background thread started executing updateAndSave
    requestStarted.await()
    assertTrue(testNavigator.popRouteEvents.isEmpty)

    // Release response: unblocks background thread
    responseRelease.complete(Unit)

    // Await navigator event Channel
    val poppedEntryId = testNavigator.popRouteEvents.receive()
    assertEquals("edit_1", poppedEntryId)
    assertTrue(testNavigator.popRouteEvents.isEmpty)
  }
}
