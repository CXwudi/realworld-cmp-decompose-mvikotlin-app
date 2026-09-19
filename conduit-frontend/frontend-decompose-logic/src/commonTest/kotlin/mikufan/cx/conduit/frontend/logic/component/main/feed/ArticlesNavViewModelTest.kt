package mikufan.cx.conduit.frontend.logic.component.main.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArticlesNavViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var storeFactory: ArticlesNavStoreFactory
  private lateinit var viewModelFactory: ArticlesNavViewModelFactory
  private lateinit var viewModelStore: ViewModelStore

  private val testFilter = ArticlesSearchFilter(tag = "kotlin")
  private val articleA = ArticleBasicInfo(
    authorThumbnail = "a.png",
    authorUsername = "alice",
    title = "Kotlin Multiplatform",
    slug = "kmp",
  )
  private val articleB = ArticleBasicInfo(
    authorThumbnail = "b.png",
    authorUsername = "bob",
    title = "Navigation 3",
    slug = "nav3",
  )

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    storeFactory = ArticlesNavStoreFactory(DefaultStoreFactory(), testDispatcher)
    viewModelFactory = ArticlesNavViewModelFactory(storeFactory)
    viewModelStore = ViewModelStore()
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun testStoreDisposedOnViewModelClear() {
    val handle = SavedStateHandle()
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(ArticlesNavViewModel::class) {
          viewModelFactory.create(handle, testFilter)
        }
      }
    )
    val vm = provider[ArticlesNavViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testNavigationMethodsAndStateForwarding(): TestResult = runTest(testDispatcher) {
    val handle = SavedStateHandle()
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(ArticlesNavViewModel::class) {
          viewModelFactory.create(handle, testFilter)
        }
      }
    )
    val vm = provider[ArticlesNavViewModel::class]

    assertEquals(testFilter, vm.searchFilter)
    assertEquals(1, vm.state.value.stack.size)
    assertTrue(vm.state.value.currentRoute is ArticlesNavRoute.List)

    // Open article A
    vm.onOpenArticle(articleA)
    testScheduler.runCurrent()
    assertEquals(2, vm.state.value.stack.size)
    val detailA = vm.state.value.currentRoute as ArticlesNavRoute.Detail
    assertEquals("detail_1", detailA.entryId)
    assertEquals(articleA, detailA.basicInfo)

    // Replace with article B
    vm.onOpenArticle(articleB)
    testScheduler.runCurrent()
    assertEquals(2, vm.state.value.stack.size)
    val detailB = vm.state.value.currentRoute as ArticlesNavRoute.Detail
    assertEquals("detail_2", detailB.entryId)
    assertEquals(articleB, detailB.basicInfo)

    // Stale close event for detail_1 rejected
    vm.onCloseDetail("detail_1")
    testScheduler.runCurrent()
    assertEquals(2, vm.state.value.stack.size)
    assertEquals("detail_2", vm.state.value.currentRoute.entryId)

    // Matching close detail accepted
    vm.onCloseDetail("detail_2")
    testScheduler.runCurrent()
    assertEquals(1, vm.state.value.stack.size)
    assertTrue(vm.state.value.currentRoute is ArticlesNavRoute.List)

    // Open article A again, then pop
    vm.onOpenArticle(articleA)
    testScheduler.runCurrent()
    assertEquals(2, vm.state.value.stack.size)

    vm.pop()
    testScheduler.runCurrent()
    assertEquals(1, vm.state.value.stack.size)
    assertTrue(vm.state.value.currentRoute is ArticlesNavRoute.List)
  }
}
