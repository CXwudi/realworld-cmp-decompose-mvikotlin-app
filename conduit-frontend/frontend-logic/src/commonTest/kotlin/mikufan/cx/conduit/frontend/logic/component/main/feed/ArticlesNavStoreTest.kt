package mikufan.cx.conduit.frontend.logic.component.main.feed

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArticlesNavStoreTest {

  private val testDispatcher = StandardTestDispatcher()
  private val storeFactory = ArticlesNavStoreFactory(DefaultStoreFactory(), testDispatcher)
  private val createdStores = mutableListOf<Store<*, *, *>>()

  private fun createStore(
    restoredSnapshot: ArticlesNavSavedSnapshot? = null,
  ): Store<ArticlesNavIntent, ArticlesNavState, Nothing> {
    val store = storeFactory.createStore(restoredSnapshot = restoredSnapshot)
    createdStores += store
    return store
  }

  @AfterTest
  fun tearDown() {
    createdStores.forEach { it.dispose() }
    createdStores.clear()
  }

  private val articleA = ArticleBasicInfo(
    authorThumbnail = "avatarA.png",
    authorUsername = "alice",
    title = "First Article",
    slug = "first-article",
  )

  private val articleB = ArticleBasicInfo(
    authorThumbnail = "avatarB.png",
    authorUsername = "bob",
    title = "Second Article",
    slug = "second-article",
  )

  @Test
  fun testInitialStateHasRootListRoute() {
    val store = createStore()
    val state = store.state

    assertEquals(1, state.stack.size)
    assertTrue(state.currentRoute is ArticlesNavRoute.List)
    assertEquals("list", state.currentRoute.entryId)
    assertEquals(1L, state.nextEntryId)
    assertFalse(state.isDetailOpen)
  }

  @Test
  fun testOpenArticlePushesDetailWithUniqueEntryId() {
    val store = createStore()

    store.accept(ArticlesNavIntent.OpenArticle(articleA))
    val state = store.state

    assertEquals(2, state.stack.size)
    assertTrue(state.stack[0] is ArticlesNavRoute.List)
    val detail = state.stack[1]
    assertTrue(detail is ArticlesNavRoute.Detail)
    assertEquals("detail_1", detail.entryId)
    assertEquals(articleA, detail.basicInfo)
    assertEquals(2L, state.nextEntryId)
    assertTrue(state.isDetailOpen)
  }

  @Test
  fun testSelectingAnotherArticleReplacesDetailPreservingList() {
    val store = createStore()

    // Open article A -> detail_1
    store.accept(ArticlesNavIntent.OpenArticle(articleA))
    assertEquals(2, store.state.stack.size)
    assertEquals("detail_1", store.state.currentRoute.entryId)

    // Select article B -> replaces detail_1 with detail_2, preserving root list
    store.accept(ArticlesNavIntent.OpenArticle(articleB))
    val state = store.state

    assertEquals(2, state.stack.size)
    assertTrue(state.stack[0] is ArticlesNavRoute.List)
    val detail = state.stack[1]
    assertTrue(detail is ArticlesNavRoute.Detail)
    assertEquals("detail_2", detail.entryId)
    assertEquals(articleB, detail.basicInfo)
    assertEquals(3L, state.nextEntryId)
  }

  @Test
  fun testDuplicateSelectionOfSameArticleIsNoOp() {
    val store = createStore()

    // Open article A
    store.accept(ArticlesNavIntent.OpenArticle(articleA))
    val stateAfterFirstOpen = store.state
    assertEquals(2, stateAfterFirstOpen.stack.size)
    assertEquals("detail_1", stateAfterFirstOpen.currentRoute.entryId)
    assertEquals(2L, stateAfterFirstOpen.nextEntryId)

    // Re-select article A with identical slug
    store.accept(ArticlesNavIntent.OpenArticle(articleA))
    val stateAfterDuplicate = store.state

    // Stack and nextEntryId must not change or reset
    assertEquals(stateAfterFirstOpen, stateAfterDuplicate)
    assertEquals("detail_1", stateAfterDuplicate.currentRoute.entryId)
    assertEquals(2L, stateAfterDuplicate.nextEntryId)
  }

  @Test
  fun testCloseDetailWithMatchingEntryIdClosesDetail() {
    val store = createStore()

    store.accept(ArticlesNavIntent.OpenArticle(articleA))
    assertEquals(2, store.state.stack.size)

    store.accept(ArticlesNavIntent.CloseDetail("detail_1"))
    val state = store.state

    assertEquals(1, state.stack.size)
    assertTrue(state.currentRoute is ArticlesNavRoute.List)
    assertFalse(state.isDetailOpen)
  }

  @Test
  fun testStaleCloseDetailRejected() {
    val store = createStore()

    // Open article A -> detail_1
    store.accept(ArticlesNavIntent.OpenArticle(articleA))
    assertEquals("detail_1", store.state.currentRoute.entryId)

    // Replace with article B -> detail_2
    store.accept(ArticlesNavIntent.OpenArticle(articleB))
    assertEquals("detail_2", store.state.currentRoute.entryId)

    // Stale close event from detail_1 arrives while detail_2 is active -> rejected
    store.accept(ArticlesNavIntent.CloseDetail("detail_1"))
    assertEquals(2, store.state.stack.size)
    assertEquals("detail_2", store.state.currentRoute.entryId)

    // Close detail_2 with matching ID -> accepted
    store.accept(ArticlesNavIntent.CloseDetail("detail_2"))
    assertEquals(1, store.state.stack.size)
    assertTrue(store.state.currentRoute is ArticlesNavRoute.List)

    // Duplicate close of detail_2 after it's gone -> rejected
    store.accept(ArticlesNavIntent.CloseDetail("detail_2"))
    assertEquals(1, store.state.stack.size)
  }

  @Test
  fun testPopIntentClosesDetailAndCannotPopRootList() {
    val store = createStore()

    // Pop on root list -> no-op
    store.accept(ArticlesNavIntent.Pop)
    assertEquals(1, store.state.stack.size)
    assertTrue(store.state.currentRoute is ArticlesNavRoute.List)

    // Open article and pop via Pop intent
    store.accept(ArticlesNavIntent.OpenArticle(articleA))
    assertEquals(2, store.state.stack.size)

    store.accept(ArticlesNavIntent.Pop)
    assertEquals(1, store.state.stack.size)
    assertTrue(store.state.currentRoute is ArticlesNavRoute.List)

    // Second pop again -> no-op
    store.accept(ArticlesNavIntent.Pop)
    assertEquals(1, store.state.stack.size)
  }

  @Test
  fun testSnapshotRestorationAndTopologyGuard() {
    val validSnapshot = ArticlesNavSavedSnapshot(
      stack = listOf(ArticlesNavRoute.List("list"), ArticlesNavRoute.Detail(articleA, "detail_1")),
      nextEntryId = 2L,
    )

    val restoredStore = createStore(restoredSnapshot = validSnapshot)
    assertEquals(2, restoredStore.state.stack.size)
    assertEquals("detail_1", restoredStore.state.currentRoute.entryId)
    assertEquals(articleA, (restoredStore.state.currentRoute as ArticlesNavRoute.Detail).basicInfo)
    assertEquals(2L, restoredStore.state.nextEntryId)

    // Invalid: empty stack -> falls back to default
    val emptySnapshot = ArticlesNavSavedSnapshot(stack = emptyList(), nextEntryId = 3L)
    val emptyRestored = createStore(restoredSnapshot = emptySnapshot)
    assertEquals(1, emptyRestored.state.stack.size)
    assertTrue(emptyRestored.state.currentRoute is ArticlesNavRoute.List)

    // Invalid: root is not List -> falls back
    val nonListRootSnapshot = ArticlesNavSavedSnapshot(
      stack = listOf(ArticlesNavRoute.Detail(articleA, "detail_1")),
      nextEntryId = 2L,
    )
    val nonListRestored = createStore(restoredSnapshot = nonListRootSnapshot)
    assertEquals(1, nonListRestored.state.stack.size)
    assertTrue(nonListRestored.state.currentRoute is ArticlesNavRoute.List)

    // Invalid: size > 2 -> falls back
    val oversizedSnapshot = ArticlesNavSavedSnapshot(
      stack = listOf(
        ArticlesNavRoute.List("list"),
        ArticlesNavRoute.Detail(articleA, "detail_1"),
        ArticlesNavRoute.Detail(articleB, "detail_2"),
      ),
      nextEntryId = 3L,
    )
    val oversizedRestored = createStore(restoredSnapshot = oversizedSnapshot)
    assertEquals(1, oversizedRestored.state.stack.size)
    assertTrue(oversizedRestored.state.currentRoute is ArticlesNavRoute.List)

    // Invalid: duplicate entryIds -> falls back
    val duplicateIdSnapshot = ArticlesNavSavedSnapshot(
      stack = listOf(ArticlesNavRoute.List("list"), ArticlesNavRoute.Detail(articleA, "list")),
      nextEntryId = 2L,
    )
    val duplicateIdRestored = createStore(restoredSnapshot = duplicateIdSnapshot)
    assertEquals(1, duplicateIdRestored.state.stack.size)
    assertTrue(duplicateIdRestored.state.currentRoute is ArticlesNavRoute.List)
  }
}
