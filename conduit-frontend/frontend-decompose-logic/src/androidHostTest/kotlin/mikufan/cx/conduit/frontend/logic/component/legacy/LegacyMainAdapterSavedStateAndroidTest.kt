package mikufan.cx.conduit.frontend.logic.component.legacy

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android host test wrapper using Robolectric for real Android Bundle/SavedState behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class LegacyMainAdapterSavedStateAndroidTest {
  @Test
  fun testSavedStateRoundtripAndroidRobolectric() {
    assertSavedStateRoundtrip()
  }
}
