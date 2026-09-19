package mikufan.cx.conduit.frontend.logic.component.main.feed

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.calls
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import mikufan.cx.conduit.frontend.logic.service.main.ArticleDetailService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleDetailViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var articleDetailService: ArticleDetailService
  private lateinit var storeFactory: ArticleDetailStoreFactory
  private lateinit var viewModelFactory: ArticleDetailViewModelFactory
  private lateinit var viewModelStore: ViewModelStore
  private lateinit var testNavigator: TestNavigator

  private val testBasicInfo = ArticleBasicInfo(
    authorThumbnail = "https://example.com/avatar.png",
    authorUsername = "testauthor",
    title = "Test Article Title",
    slug = "test-article-slug",
  )

  private val testDetailInfo = ArticleDetailInfo(
    description = "Test Description",
    bodyMarkdown = "# Markdown content",
    tags = listOf("kotlin", "kmp"),
    createdAt = Instant.parse("2024-01-01T00:00:00Z"),
  )

  private class TestNavigator : ArticlesNavNavigator {
    val openArticleEvents = Channel<ArticleBasicInfo>(Channel.UNLIMITED)
    val closeDetailEvents = Channel<String>(Channel.UNLIMITED)

    override fun onOpenArticle(basicInfo: ArticleBasicInfo) {
      openArticleEvents.trySend(basicInfo)
    }

    override fun onCloseDetail(entryId: String) {
      closeDetailEvents.trySend(entryId)
    }
  }

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    articleDetailService = mock()
    storeFactory = ArticleDetailStoreFactory(DefaultStoreFactory(), articleDetailService, testDispatcher)
    viewModelFactory = ArticleDetailViewModelFactory(storeFactory)
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
    everySuspend { articleDetailService.getArticle(testBasicInfo.slug) } calls { testDetailInfo }

    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(ArticleDetailViewModel::class) {
          viewModelFactory.create(testBasicInfo, "detail_1", testNavigator)
        }
      }
    )
    val vm = provider[ArticleDetailViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testImmediateBackToListWithoutPreRunCurrent(): TestResult = runTest(testDispatcher) {
    everySuspend { articleDetailService.getArticle(testBasicInfo.slug) } calls { testDetailInfo }

    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(ArticleDetailViewModel::class) {
          viewModelFactory.create(testBasicInfo, "detail_1", testNavigator)
        }
      }
    )
    val vm = provider[ArticleDetailViewModel::class]

    // Immediately send back intent without pre-runCurrent:
    // Synchronously installed labelsChannel ensures label is buffered
    vm.send(ArticleDetailIntent.BackToList)
    testScheduler.runCurrent()

    assertEquals("detail_1", testNavigator.closeDetailEvents.receive())
  }

  @Test
  fun testDeferredDetailLoadingLoadsContent(): TestResult = runTest(testDispatcher) {
    val requestStarted = CompletableDeferred<Unit>()
    val responseRelease = CompletableDeferred<Unit>()

    everySuspend { articleDetailService.getArticle(testBasicInfo.slug) } calls {
      requestStarted.complete(Unit)
      responseRelease.await()
      testDetailInfo
    }

    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(ArticleDetailViewModel::class) {
          viewModelFactory.create(testBasicInfo, "detail_1", testNavigator)
        }
      }
    )
    val vm = provider[ArticleDetailViewModel::class]

    // Bootstrapper starts loading
    requestStarted.await()
    assertEquals(testBasicInfo, vm.state.value.basicInfo)
    assertEquals(null, vm.state.value.detailInfo)

    // Complete response
    responseRelease.complete(Unit)

    // Await actual StateFlow condition returning from background thread
    val detailState = vm.state.first { it.detailInfo == testDetailInfo }
    assertEquals(testDetailInfo, detailState.detailInfo)
    assertEquals(testBasicInfo, detailState.basicInfo)
  }
}
