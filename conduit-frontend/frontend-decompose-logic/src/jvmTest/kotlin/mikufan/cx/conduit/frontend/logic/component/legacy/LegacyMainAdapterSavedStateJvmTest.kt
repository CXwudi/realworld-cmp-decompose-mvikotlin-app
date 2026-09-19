package mikufan.cx.conduit.frontend.logic.component.legacy

import kotlin.test.Test

/**
 * JVM test wrapper for native SavedState roundtrip.
 */
class LegacyMainAdapterSavedStateJvmTest {
  @Test
  fun testSavedStateRoundtripJvm() {
    assertSavedStateRoundtrip()
  }
}
