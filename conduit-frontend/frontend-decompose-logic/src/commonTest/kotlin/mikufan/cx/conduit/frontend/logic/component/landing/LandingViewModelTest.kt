package mikufan.cx.conduit.frontend.logic.component.landing

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mikufan.cx.conduit.frontend.logic.service.landing.LandingService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests native ViewModel lifecycle ownership and disposal for [LandingViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LandingViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var landingService: LandingService
  private lateinit var storeFactory: LandingPageStoreFactory
  private lateinit var viewModelFactory: LandingViewModelFactory
  private lateinit var viewModelStore: ViewModelStore

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    landingService = mock()
    storeFactory = LandingPageStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      landingService,
      testDispatcher,
    )
    viewModelFactory = LandingViewModelFactory(storeFactory)
    viewModelStore = ViewModelStore()
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  private fun createLandingViewModel(): LandingViewModel {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(LandingViewModel::class) {
          viewModelFactory.create()
        }
      }
    )
    return provider[LandingViewModel::class]
  }

  @Test
  fun testViewModelStoreOwnershipAndSingleInstance() {
    val vm1 = createLandingViewModel()
    val vm2 = createLandingViewModel()

    // Same ViewModelStore returns the exact same instance
    assertSame(vm1, vm2)
    assertFalse(vm1.isStoreDisposed)
  }

  @Test
  fun testStateAndIntentForwarding() = runTest(testDispatcher) {
    val vm = createLandingViewModel()
    testScheduler.runCurrent()
    assertEquals("", vm.state.value.url)

    vm.send(LandingPageIntent.TextChanged("https://example.com/api"))
    testScheduler.runCurrent()
    assertEquals("https://example.com/api", vm.state.value.url)
    assertFalse(vm.isStoreDisposed)
  }

  @Test
  fun testClearDisposesUnderlyingStore() {
    val vm = createLandingViewModel()
    assertFalse(vm.isStoreDisposed)

    // Clearing ViewModelStore triggers onCleared and disposes store
    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }
}
