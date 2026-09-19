package mikufan.cx.conduit.frontend.logic.component.legacy

import kotlin.test.Test

/**
 * JS test wrapper for native SavedState roundtrip.
 */
class LegacyMainAdapterSavedStateJsTest {
  @Test
  fun testSavedStateRoundtripJs() {
    assertSavedStateRoundtrip()
  }
}
