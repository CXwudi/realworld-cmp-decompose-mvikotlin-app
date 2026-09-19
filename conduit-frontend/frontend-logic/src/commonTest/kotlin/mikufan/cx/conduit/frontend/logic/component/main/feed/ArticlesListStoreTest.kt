package mikufan.cx.conduit.frontend.logic.component.main.feed

import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.labelsChannel
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import mikufan.cx.conduit.frontend.logic.service.main.ArticlesListService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticlesListStoreTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var articlesListService: ArticlesListService
  private lateinit var articlesListStore: Store<ArticlesListIntent, ArticlesListState, ArticlesListLabel>
  
  private val defaultSearchFilter = ArticlesSearchFilter()
  private val testArticles = listOf(
    ArticleInfo(
      authorThumbnail = "https://example.com/avatar.png",
      authorUsername = "testuser",
      title = "Test Article",
      description = "Test Description",
      tags = listOf("test", "kotlin"),
      createdAt = Instant.parse("2023-01-01T12:00:00Z"),
      slug = "test-article"
    ),
    ArticleInfo(
      authorThumbnail = null,
      authorUsername = "anotheruser",
      title = "Another Test Article",
      description = "Another Description",
      tags = listOf("test", "mvikotlin"),
      createdAt = Instant.parse("2023-01-02T12:00:00Z"),
      slug = "another-test-article"
    )
  )

  @BeforeTest
  fun setUp() {
    articlesListService = mock()
    articlesListStore = ArticlesListStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      articlesListService,
      testDispatcher,
    ).create(defaultSearchFilter)
  }

  @AfterTest
  fun reset() {
    articlesListStore.dispose()
  }

  @Test
  fun testInitialState() = runTest(testDispatcher) {
    // the initial state should have empty list and LoadMoreState.Loaded
    assertEquals(emptyList<ArticleInfo>(), articlesListStore.state.collectedThumbInfos)
    assertEquals(LoadMoreState.Loaded, articlesListStore.state.loadMoreState)
  }

  @Test
  fun testLoadMoreSuccess() = runTest(testDispatcher) {
    val stateChannel = Channel<ArticlesListState>()
    
    // Given
    everySuspend {
      articlesListService.getArticles(defaultSearchFilter, offset = 0)
    } returns testArticles

    // When
    val disposable = articlesListStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    
    // Initial state
    val initialState = stateChannel.receive()
    assertEquals(emptyList<ArticleInfo>(), initialState.collectedThumbInfos)
    assertEquals(LoadMoreState.Loaded, initialState.loadMoreState)
    
    // Send LoadMore intent
    articlesListStore.accept(ArticlesListIntent.LoadMore)

    // Then - should transition to loading state
    val loadingState = stateChannel.receive()
    assertEquals(emptyList<ArticleInfo>(), loadingState.collectedThumbInfos)
    assertEquals(LoadMoreState.Loading, loadingState.loadMoreState)

    // Then - should transition to loaded state with articles
    val loadedState = stateChannel.receive()
    assertEquals(testArticles, loadedState.collectedThumbInfos)
    assertEquals(LoadMoreState.Loaded, loadedState.loadMoreState)

    // Verify service called correctly
    verifySuspend(exactly(1)) { articlesListService.getArticles(defaultSearchFilter, offset = 0) }

    disposable.dispose()
  }

  @Test
  fun testLoadMoreFailure() = runTest(testDispatcher) {
    val stateChannel = Channel<ArticlesListState>()
    val testScope = TestScope(testDispatcher)
    val labelChannel = articlesListStore.labelsChannel(testScope)

    // Given
    val errorMessage = "Failed to load articles"
    everySuspend {
      articlesListService.getArticles(any(), offset = any())
    } throws RuntimeException(errorMessage)

    // When
    val disposable = articlesListStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    
    // Initial state
    stateChannel.receive()
    
    // Send LoadMore intent
    articlesListStore.accept(ArticlesListIntent.LoadMore)

    // Then - should transition to loading state
    val loadingState = stateChannel.receive()
    assertEquals(LoadMoreState.Loading, loadingState.loadMoreState)

    // Then - should transition back to loaded state with empty list
    val finalState = stateChannel.receive()
    assertEquals(emptyList<ArticleInfo>(), finalState.collectedThumbInfos)
    assertEquals(LoadMoreState.Loaded, finalState.loadMoreState)

    // Should emit a failure label
    val failureLabel = labelChannel.receive() as ArticlesListLabel.Failure
    assertEquals(errorMessage, failureLabel.message)
    assertTrue(failureLabel.exception is RuntimeException)

    // Verify service called
    verifySuspend(exactly(1)) { articlesListService.getArticles(any(), offset = any()) }

    disposable.dispose()
  }

  @Test
  fun testLoadMoreWithExistingArticles() = runTest(testDispatcher) {
    val stateChannel = Channel<ArticlesListState>()
    val moreArticles = listOf(
      ArticleInfo(
        authorThumbnail = "https://example.com/avatar3.png",
        authorUsername = "thirduser",
        title = "Third Article",
        description = "More test content",
        tags = listOf("testing"),
        createdAt = Instant.parse("2023-01-03T12:00:00Z"),
        slug = "third-article"
      )
    )

    // Given
    everySuspend {
      articlesListService.getArticles(defaultSearchFilter, offset = 0)
    } returns testArticles

    everySuspend {
      articlesListService.getArticles(defaultSearchFilter, offset = testArticles.size)
    } returns moreArticles

    // When
    val disposable = articlesListStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    
    // Initial state
    stateChannel.receive()
    
    // First load
    articlesListStore.accept(ArticlesListIntent.LoadMore)
    stateChannel.receive() // Loading state
    val firstLoadedState = stateChannel.receive() 
    assertEquals(testArticles, firstLoadedState.collectedThumbInfos)
    assertEquals(LoadMoreState.Loaded, firstLoadedState.loadMoreState)

    // Second load
    articlesListStore.accept(ArticlesListIntent.LoadMore)

    // Then - should transition to loading state
    val loadingMoreState = stateChannel.receive()
    assertEquals(LoadMoreState.Loading, loadingMoreState.loadMoreState)
    assertEquals(testArticles, loadingMoreState.collectedThumbInfos)

    // Then - should transition to loaded state with more articles
    val finalState = stateChannel.receive()
    assertEquals(LoadMoreState.Loaded, finalState.loadMoreState)
    assertEquals(testArticles + moreArticles, finalState.collectedThumbInfos)

    // Verify service calls
    verifySuspend(exactly(1)) { articlesListService.getArticles(defaultSearchFilter, offset = 0) }
    verifySuspend(exactly(1)) {
      articlesListService.getArticles(defaultSearchFilter, offset = testArticles.size)
    }

    disposable.dispose()
  }
}
