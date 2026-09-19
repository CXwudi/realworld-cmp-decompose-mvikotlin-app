package mikufan.cx.conduit.frontend.logic.component.legacy

import kotlin.test.Test

/**
 * WasmJs test wrapper for native SavedState roundtrip.
 */
class LegacyMainAdapterSavedStateWasmTest {
  @Test
  fun testSavedStateRoundtripWasm() {
    assertSavedStateRoundtrip()
  }
}
