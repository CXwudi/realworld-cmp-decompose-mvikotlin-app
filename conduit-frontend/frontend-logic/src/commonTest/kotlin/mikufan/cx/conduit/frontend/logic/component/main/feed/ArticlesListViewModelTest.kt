package mikufan.cx.conduit.frontend.logic.component.main.feed

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import mikufan.cx.conduit.frontend.logic.service.main.ArticlesListService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArticlesListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var articlesListService: ArticlesListService
  private lateinit var storeFactory: ArticlesListStoreFactory
  private lateinit var viewModelFactory: ArticlesListViewModelFactory
  private lateinit var viewModelStore: ViewModelStore
  private lateinit var testNavigator: TestNavigator

  private val filter = ArticlesSearchFilter()

  private val testArticles = listOf(
    ArticleInfo(
      authorThumbnail = "https://example.com/avatar.png",
      authorUsername = "testuser",
      title = "Test Article",
      description = "Test Description",
      tags = listOf("test", "kotlin"),
      createdAt = Instant.parse("2023-01-01T12:00:00Z"),
      slug = "test-article",
    )
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
    articlesListService = mock()
    storeFactory = ArticlesListStoreFactory(DefaultStoreFactory(), articlesListService, testDispatcher)
    viewModelFactory = ArticlesListViewModelFactory(storeFactory)
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
        addInitializer(ArticlesListViewModel::class) {
          viewModelFactory.create(filter, testNavigator)
        }
      }
    )
    val vm = provider[ArticlesListViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testImmediateArticleClickWithoutPreRunCurrent(): TestResult = runTest(testDispatcher) {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(ArticlesListViewModel::class) {
          viewModelFactory.create(filter, testNavigator)
        }
      }
    )
    val vm = provider[ArticlesListViewModel::class]

    val basicInfo = ArticleBasicInfo(
      authorThumbnail = "thumb.png",
      authorUsername = "alice",
      title = "Immediate Article",
      slug = "immediate-article",
    )

    // Immediately send click intent without pre-runCurrent:
    // Synchronously installed labelsChannel ensures label is buffered
    vm.send(ArticlesListIntent.ClickOnArticle(basicInfo))
    testScheduler.runCurrent()

    assertEquals(basicInfo, testNavigator.openArticleEvents.receive())
  }

  @Test
  fun testDeferredArticlesLoadingUpdatesState(): TestResult = runTest(testDispatcher) {
    val requestStarted = CompletableDeferred<Unit>()
    val responseRelease = CompletableDeferred<Unit>()

    everySuspend { articlesListService.getArticles(any(), any(), any()) } calls {
      requestStarted.complete(Unit)
      responseRelease.await()
      testArticles
    }

    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(ArticlesListViewModel::class) {
          viewModelFactory.create(filter, testNavigator)
        }
      }
    )
    val vm = provider[ArticlesListViewModel::class]

    vm.send(ArticlesListIntent.LoadMore)

    // Await background execution started and loading state in StateFlow
    requestStarted.await()
    val loadingState = vm.state.first { it.loadMoreState == LoadMoreState.Loading }
    assertEquals(LoadMoreState.Loading, loadingState.loadMoreState)

    // Release response
    responseRelease.complete(Unit)

    // Await actual StateFlow condition returning from background thread
    val loadedState = vm.state.first { it.collectedThumbInfos == testArticles }
    assertEquals(LoadMoreState.Loaded, loadedState.loadMoreState)
    assertEquals(testArticles, loadedState.collectedThumbInfos)
  }

  @Test
  fun testFeedAndFavouritesIndependenceWithSharedFactory(): TestResult = runTest(testDispatcher) {
    val capturedFilters = Channel<ArticlesSearchFilter>(Channel.UNLIMITED)
    everySuspend { articlesListService.getArticles(any(), any(), any()) } calls { (filterArg: ArticlesSearchFilter) ->
      capturedFilters.send(filterArg)
      emptyList()
    }

    val vmStoreFeed = ViewModelStore()
    val vmStoreFav = ViewModelStore()
    val feedNavigator = TestNavigator()
    val favNavigator = TestNavigator()

    try {
      val feedFilter = ArticlesSearchFilter()
      val favFilter = ArticlesSearchFilter(favoritedByUsername = "miku")

      // Use the SAME singleton factory to create independent ViewModels
      val vmFeed = ViewModelProvider.create(
        vmStoreFeed,
        viewModelFactory {
          addInitializer(ArticlesListViewModel::class) {
            viewModelFactory.create(feedFilter, feedNavigator)
          }
        }
      )[ArticlesListViewModel::class]

      val vmFav = ViewModelProvider.create(
        vmStoreFav,
        viewModelFactory {
          addInitializer(ArticlesListViewModel::class) {
            viewModelFactory.create(favFilter, favNavigator)
          }
        }
      )[ArticlesListViewModel::class]

      // Trigger LoadMore on Feed -> asserts captured filter is feedFilter
      vmFeed.send(ArticlesListIntent.LoadMore)
      assertEquals(feedFilter, capturedFilters.receive())

      // Trigger LoadMore on Fav -> asserts captured filter is favFilter
      vmFav.send(ArticlesListIntent.LoadMore)
      assertEquals(favFilter, capturedFilters.receive())

      // Navigate ONLY on Feed
      val article = ArticleBasicInfo(
        authorThumbnail = "thumb.png",
        authorUsername = "author",
        title = "Article Title",
        slug = "article-slug",
      )
      vmFeed.send(ArticlesListIntent.ClickOnArticle(article))
      testScheduler.runCurrent()

      // Feed navigator received event; Favourites navigator remained completely untouched
      assertEquals(article, feedNavigator.openArticleEvents.receive())
      assertTrue(favNavigator.openArticleEvents.isEmpty)

      // Clear only the Feed owner ViewModelStore
      vmStoreFeed.clear()
      assertTrue(vmFeed.isStoreDisposed, "Feed store must be disposed when its owner clears")
      assertFalse(vmFav.isStoreDisposed, "Favourites store must NOT be disposed when Feed clears")
    } finally {
      vmStoreFeed.clear()
      vmStoreFav.clear()
    }
  }
}
