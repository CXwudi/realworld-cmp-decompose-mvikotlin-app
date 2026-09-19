package mikufan.cx.conduit.frontend.logic.component.main.me

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MeNavStoreTest {

  private val testDispatcher = StandardTestDispatcher()
  private val storeFactory = MeNavStoreFactory(DefaultStoreFactory(), testDispatcher)
  private val createdStores = mutableListOf<Store<*, *, *>>()

  private fun createStore(restoredSnapshot: MeNavSavedSnapshot? = null): Store<MeNavIntent, MeNavState, Nothing> {
    val store = storeFactory.createStore(restoredSnapshot = restoredSnapshot)
    createdStores += store
    return store
  }

  @AfterTest
  fun tearDown() {
    createdStores.forEach { it.dispose() }
    createdStores.clear()
  }

  @Test
  fun testInitialStateHasProfileRoute() {
    val store = createStore()
    val state = store.state

    assertEquals(1, state.stack.size)
    assertTrue(state.currentRoute is MeNavRoute.Profile)
    assertEquals("profile", state.currentRoute.entryId)
    assertEquals(1L, state.nextEntryId)
  }

  @Test
  fun testNavigateToEditProfileAndRepeatedClickProtection() {
    val store = createStore()
    val loadedMe = LoadedMe(email = "alice@example.com", username = "alice", bio = "hello", imageUrl = "avatar.png")

    store.accept(MeNavIntent.NavigateToEditProfile(loadedMe))
    val stateAfterPush = store.state
    assertEquals(2, stateAfterPush.stack.size)
    assertTrue(stateAfterPush.stack[0] is MeNavRoute.Profile)
    val editRoute = stateAfterPush.stack[1]
    assertTrue(editRoute is MeNavRoute.EditProfile)
    assertEquals("edit_1", editRoute.entryId)
    assertEquals(loadedMe, editRoute.loadedMe)
    assertEquals(2L, stateAfterPush.nextEntryId)

    // Repeated click while on EditProfile -> must be ignored (repeated click protection)
    val otherLoadedMe = LoadedMe(email = "other@example.com", username = "other")
    store.accept(MeNavIntent.NavigateToEditProfile(otherLoadedMe))
    assertEquals(stateAfterPush, store.state)

    // Attempting to push AddArticle while on EditProfile -> also ignored
    store.accept(MeNavIntent.NavigateToAddArticle)
    assertEquals(stateAfterPush, store.state)
  }

  @Test
  fun testNavigateToAddArticleAndRepeatedClickProtection() {
    val store = createStore()

    store.accept(MeNavIntent.NavigateToAddArticle)
    val stateAfterPush = store.state
    assertEquals(2, stateAfterPush.stack.size)
    assertTrue(stateAfterPush.stack[0] is MeNavRoute.Profile)
    val addRoute = stateAfterPush.stack[1]
    assertTrue(addRoute is MeNavRoute.AddArticle)
    assertEquals("add_1", addRoute.entryId)
    assertEquals(2L, stateAfterPush.nextEntryId)

    // Repeated click while on AddArticle -> ignored
    store.accept(MeNavIntent.NavigateToAddArticle)
    assertEquals(stateAfterPush, store.state)

    val loadedMe = LoadedMe(email = "alice@example.com", username = "alice")
    store.accept(MeNavIntent.NavigateToEditProfile(loadedMe))
    assertEquals(stateAfterPush, store.state)
  }

  @Test
  fun testPopRouteWithMatchingEntryIdAndUniqueReopenIdentity() {
    val store = createStore()
    val loadedMe = LoadedMe(email = "alice@example.com", username = "alice")

    // Push EditProfile -> edit_1
    store.accept(MeNavIntent.NavigateToEditProfile(loadedMe))
    assertEquals(2, store.state.stack.size)
    assertEquals("edit_1", store.state.currentRoute.entryId)

    // Pop with matching entryId
    store.accept(MeNavIntent.PopRoute("edit_1"))
    assertEquals(1, store.state.stack.size)
    assertTrue(store.state.currentRoute is MeNavRoute.Profile)

    // Reopen editor -> gets fresh unique entryId "edit_2"
    store.accept(MeNavIntent.NavigateToEditProfile(loadedMe))
    assertEquals(2, store.state.stack.size)
    assertEquals("edit_2", store.state.currentRoute.entryId)
    assertEquals(3L, store.state.nextEntryId)
  }

  @Test
  fun testStaleAndDuplicateCompletionRejection() {
    val store = createStore()
    val loadedMe = LoadedMe(email = "alice@example.com", username = "alice")

    // Push edit_1
    store.accept(MeNavIntent.NavigateToEditProfile(loadedMe))
    assertEquals("edit_1", store.state.currentRoute.entryId)

    // User pops edit_1 via back
    store.accept(MeNavIntent.Pop)
    assertEquals(1, store.state.stack.size)

    // Stale completion from edit_1 arrives after it was popped -> must be rejected
    store.accept(MeNavIntent.PopRoute("edit_1"))
    assertEquals(1, store.state.stack.size)
    assertTrue(store.state.currentRoute is MeNavRoute.Profile)

    // Now user opens edit_2
    store.accept(MeNavIntent.NavigateToEditProfile(loadedMe))
    assertEquals("edit_2", store.state.currentRoute.entryId)

    // Stale completion from old edit_1 arrives while edit_2 is active -> rejected!
    store.accept(MeNavIntent.PopRoute("edit_1"))
    assertEquals(2, store.state.stack.size)
    assertEquals("edit_2", store.state.currentRoute.entryId)

    // Now pop edit_2
    store.accept(MeNavIntent.PopRoute("edit_2"))
    assertEquals(1, store.state.stack.size)

    // Duplicate pop of edit_2 -> rejected
    store.accept(MeNavIntent.PopRoute("edit_2"))
    assertEquals(1, store.state.stack.size)
  }

  @Test
  fun testPopIntentCannotPopRootProfile() {
    val store = createStore()
    assertEquals(1, store.state.stack.size)

    // Pop when stack has only Profile -> no-op
    store.accept(MeNavIntent.Pop)
    assertEquals(1, store.state.stack.size)
    assertTrue(store.state.currentRoute is MeNavRoute.Profile)

    // Push AddArticle and pop via Pop intent
    store.accept(MeNavIntent.NavigateToAddArticle)
    assertEquals(2, store.state.stack.size)
    store.accept(MeNavIntent.Pop)
    assertEquals(1, store.state.stack.size)
    assertTrue(store.state.currentRoute is MeNavRoute.Profile)
  }

  @Test
  fun testSnapshotRestorationAndTopologyGuard() {
    val loadedMe = LoadedMe(email = "alice@example.com", username = "alice")
    val validSnapshot = MeNavSavedSnapshot(
      stack = listOf(MeNavRoute.Profile("profile"), MeNavRoute.EditProfile(loadedMe, "edit_1")),
      nextEntryId = 2L,
    )

    val restoredStore = createStore(restoredSnapshot = validSnapshot)
    assertEquals(2, restoredStore.state.stack.size)
    assertEquals("edit_1", restoredStore.state.currentRoute.entryId)
    assertEquals(2L, restoredStore.state.nextEntryId)

    // Invalid topology: empty stack -> falls back to default Profile
    val emptySnapshot = MeNavSavedSnapshot(stack = emptyList(), nextEntryId = 5L)
    val emptyRestored = createStore(restoredSnapshot = emptySnapshot)
    assertEquals(1, emptyRestored.state.stack.size)
    assertTrue(emptyRestored.state.currentRoute is MeNavRoute.Profile)

    // Invalid topology: does not start with Profile -> falls back
    val invalidStartSnapshot = MeNavSavedSnapshot(
      stack = listOf(MeNavRoute.AddArticle("add_1")),
      nextEntryId = 2L,
    )
    val invalidStartRestored = createStore(restoredSnapshot = invalidStartSnapshot)
    assertEquals(1, invalidStartRestored.state.stack.size)
    assertTrue(invalidStartRestored.state.currentRoute is MeNavRoute.Profile)

    // Invalid topology: size > 2 -> falls back
    val oversizedSnapshot = MeNavSavedSnapshot(
      stack = listOf(MeNavRoute.Profile("profile"), MeNavRoute.AddArticle("add_1"), MeNavRoute.EditProfile(loadedMe, "edit_2")),
      nextEntryId = 3L,
    )
    val oversizedRestored = createStore(restoredSnapshot = oversizedSnapshot)
    assertEquals(1, oversizedRestored.state.stack.size)
    assertTrue(oversizedRestored.state.currentRoute is MeNavRoute.Profile)

    // Invalid topology: duplicate entryIds -> falls back
    val duplicateIdSnapshot = MeNavSavedSnapshot(
      stack = listOf(MeNavRoute.Profile("profile"), MeNavRoute.AddArticle("profile")),
      nextEntryId = 2L,
    )
    val duplicateIdRestored = createStore(restoredSnapshot = duplicateIdSnapshot)
    assertEquals(1, duplicateIdRestored.state.stack.size)
    assertTrue(duplicateIdRestored.state.currentRoute is MeNavRoute.Profile)
  }
}
