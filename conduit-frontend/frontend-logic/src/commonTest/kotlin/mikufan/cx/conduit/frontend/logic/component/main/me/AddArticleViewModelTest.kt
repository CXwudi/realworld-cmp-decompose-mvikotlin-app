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
import mikufan.cx.conduit.frontend.logic.service.main.AddArticleService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddArticleViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var addArticleService: AddArticleService
  private lateinit var storeFactory: AddArticleStoreFactory
  private lateinit var viewModelFactory: AddArticleViewModelFactory
  private lateinit var viewModelStore: ViewModelStore
  private lateinit var testNavigator: TestNavigator

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
    addArticleService = mock()
    storeFactory = AddArticleStoreFactory(DefaultStoreFactory(), addArticleService, testDispatcher)
    viewModelFactory = AddArticleViewModelFactory(storeFactory)
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
        addInitializer(AddArticleViewModel::class) {
          viewModelFactory.create("add_1", testNavigator)
        }
      }
    )
    val vm = provider[AddArticleViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testImmediateBackWithoutPreRunCurrent() = runTest(testDispatcher) {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(AddArticleViewModel::class) {
          viewModelFactory.create("add_1", testNavigator)
        }
      }
    )
    val vm = provider[AddArticleViewModel::class]

    // Immediately send intent without calling runCurrent first:
    // Synchronously installed labelsChannel ensures label is buffered without pre-runCurrent
    vm.send(AddArticleIntent.BackWithoutPublish)
    testScheduler.runCurrent()

    assertEquals("add_1", testNavigator.popRouteEvents.receive())
  }

  @Test
  fun testDeferredPublishNavigatesWithoutUiCollector() = runTest(testDispatcher) {
    val requestStarted = CompletableDeferred<Unit>()
    val responseRelease = CompletableDeferred<Unit>()
    everySuspend { addArticleService.createArticle(any(), any(), any(), any()) } calls {
      requestStarted.complete(Unit)
      responseRelease.await()
    }

    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(AddArticleViewModel::class) {
          viewModelFactory.create("add_1", testNavigator)
        }
      }
    )
    val vm = provider[AddArticleViewModel::class]

    // Populate article fields and publish
    vm.send(AddArticleIntent.TitleChanged("My New Post"))
    vm.send(AddArticleIntent.DescriptionChanged("Post summary"))
    vm.send(AddArticleIntent.BodyChanged("Full article body text"))
    vm.send(AddArticleIntent.Publish)

    // Await requestStarted: ensures background thread started executing createArticle
    requestStarted.await()
    assertTrue(testNavigator.popRouteEvents.isEmpty)

    // Release response: unblocks background thread
    responseRelease.complete(Unit)

    // Await navigator event Channel
    val poppedEntryId = testNavigator.popRouteEvents.receive()
    assertEquals("add_1", poppedEntryId)
    assertTrue(testNavigator.popRouteEvents.isEmpty)
  }
}
