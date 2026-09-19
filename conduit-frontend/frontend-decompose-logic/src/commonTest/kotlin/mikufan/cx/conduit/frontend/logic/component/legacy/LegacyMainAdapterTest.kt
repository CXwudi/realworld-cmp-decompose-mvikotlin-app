package mikufan.cx.conduit.frontend.logic.component.legacy

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.lifecycle.Lifecycle as EssentyLifecycle
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponent
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponentFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class TestInstance : InstanceKeeper.Instance {
  var isDestroyed = false
  override fun onDestroy() {
    isDestroyed = true
  }
}

/**
 * Multiplatform focused tests for [LegacyMainAdapterViewModel]:
 * - Safe lifecycle bridging across configuration changes (rotation).
 * - Lifecycle reattachment after simulated rotation.
 * - Destruction only on native ViewModel clear.
 * - Clear-before-disposal ordering safety through production synchronization methods.
 * - Back dispatching.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LegacyMainAdapterTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockMainNavComponent: MainNavComponent
  private lateinit var mainNavComponentFactory: MainNavComponentFactory
  private lateinit var viewModelStore: ViewModelStore

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockMainNavComponent = mock()
    mainNavComponentFactory = MainNavComponentFactory { mockMainNavComponent }
    viewModelStore = ViewModelStore()
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  private fun createAdapterViewModel(handle: SavedStateHandle = SavedStateHandle()): LegacyMainAdapterViewModel {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(LegacyMainAdapterViewModel::class) {
          LegacyMainAdapterViewModel(mainNavComponentFactory, handle)
        }
      }
    )
    return provider[LegacyMainAdapterViewModel::class]
  }

  @Test
  fun testRetentionBoundaryAndReattachmentAcrossRotation() {
    val adapter = createAdapterViewModel()

    // 1. Initial state
    assertEquals(EssentyLifecycle.State.INITIALIZED, adapter.lifecycleRegistry.state)

    // 2. Drive to RESUMED when displayed
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_CREATE)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_START)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_RESUME)
    assertEquals(EssentyLifecycle.State.RESUMED, adapter.lifecycleRegistry.state)

    // 3. Simulate rotation / config change: Activity is destroyed, host disposes
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_STOP)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    adapter.onHostDisposed()
    assertEquals(EssentyLifecycle.State.CREATED, adapter.lifecycleRegistry.state)

    // CRITICAL: Retention boundary check - must NOT be DESTROYED
    assertTrue(adapter.lifecycleRegistry.state != EssentyLifecycle.State.DESTROYED)

    // 4. Simulate recreation after rotation: new Activity reattaches to the SAME retained ViewModel
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_START)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_RESUME)
    assertEquals(EssentyLifecycle.State.RESUMED, adapter.lifecycleRegistry.state)
  }

  @Test
  fun testDestructionOnlyWhenViewModelClears() {
    val adapter = createAdapterViewModel()

    val testInstance = TestInstance()
    adapter.instanceKeeper.put("test_key", testInstance)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_CREATE)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_START)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_RESUME)

    assertFalse(testInstance.isDestroyed)
    assertEquals(EssentyLifecycle.State.RESUMED, adapter.lifecycleRegistry.state)

    // Simulate composition disposal without clearing VM (e.g. rotation)
    adapter.onHostDisposed()
    assertFalse(testInstance.isDestroyed)
    assertEquals(EssentyLifecycle.State.CREATED, adapter.lifecycleRegistry.state)

    // True destruction happens when the native ViewModelStore clears
    viewModelStore.clear()
    assertEquals(EssentyLifecycle.State.DESTROYED, adapter.lifecycleRegistry.state)
    assertTrue(testInstance.isDestroyed)
  }

  @Test
  fun testClearBeforeDisposalOrderingSafety() {
    val adapter = createAdapterViewModel()
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_CREATE)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_START)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_RESUME)
    assertEquals(EssentyLifecycle.State.RESUMED, adapter.lifecycleRegistry.state)

    // 1. Native ViewModel clears FIRST (e.g. NavDisplay popped entry before host disposal effect ran)
    viewModelStore.clear()
    assertEquals(EssentyLifecycle.State.DESTROYED, adapter.lifecycleRegistry.state)

    // 2. Production lifecycle synchronization and disposal called on already-destroyed adapter must be safe and not throw
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_STOP)
    adapter.syncLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    adapter.onHostDisposed()
    assertEquals(EssentyLifecycle.State.DESTROYED, adapter.lifecycleRegistry.state)
  }

  @Test
  fun testBackDispatching() {
    val adapter = createAdapterViewModel()

    var backHandled = false
    adapter.backDispatcher.register(
      com.arkivanov.essenty.backhandler.BackCallback {
        backHandled = true
      }
    )

    assertTrue(adapter.backDispatcher.isEnabled)
    val result = adapter.handleBack()
    assertTrue(result)
    assertTrue(backHandled)
  }
}
