package mikufan.cx.conduit.frontend.logic.component.legacy

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.mokkery.mock
import kotlinx.serialization.Serializable
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponent
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponentFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Serializable
data class TestSavedPayload(val message: String)

/**
 * Shared test helper executing actual native [SavedStateHandle] roundtrip.
 * Does NOT carry @Test annotation. Invoked by target-specific wrappers:
 * - JVM: LegacyMainAdapterSavedStateJvmTest
 * - JS: LegacyMainAdapterSavedStateJsTest
 * - Wasm: LegacyMainAdapterSavedStateWasmTest
 * - Android: LegacyMainAdapterSavedStateAndroidTest (Robolectric)
 */
fun assertSavedStateRoundtrip() {
  val mockMainNavComponent: MainNavComponent = mock()
  val mainNavComponentFactory = MainNavComponentFactory { mockMainNavComponent }

  val store1 = ViewModelStore()
  val handle1 = SavedStateHandle()
  val provider1 = ViewModelProvider.create(
    store1,
    viewModelFactory {
      addInitializer(LegacyMainAdapterViewModel::class) {
        LegacyMainAdapterViewModel(mainNavComponentFactory, handle1)
      }
    }
  )
  val adapter1 = provider1[LegacyMainAdapterViewModel::class]

  // 1. Register a test payload in adapter1's state keeper
  adapter1.stateKeeper.register("test_key", TestSavedPayload.serializer()) {
    TestSavedPayload("restored_nav_state_42")
  }

  // 2. Perform snapshot via actual SavedState machinery
  val savedState = handle1.savedStateProvider().saveState()
  assertNotNull(savedState)

  // 3. Clear store 1 to simulate process death / destruction
  store1.clear()

  // 4. Create restored SavedStateHandle from the saved state in a new store
  val store2 = ViewModelStore()
  val handle2 = SavedStateHandle.createHandle(savedState, null)
  val provider2 = ViewModelProvider.create(
    store2,
    viewModelFactory {
      addInitializer(LegacyMainAdapterViewModel::class) {
        LegacyMainAdapterViewModel(mainNavComponentFactory, handle2)
      }
    }
  )
  val adapter2 = provider2[LegacyMainAdapterViewModel::class]

  // 5. Verify that adapter2's state keeper has restored the state
  val restored = adapter2.stateKeeper.consume("test_key", TestSavedPayload.serializer())
  assertNotNull(restored)
  assertEquals("restored_nav_state_42", restored.message)

  store2.clear()
}
