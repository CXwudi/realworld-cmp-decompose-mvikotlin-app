package mikufan.cx.conduit.frontend.logic.component.main.me

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class MeNavViewModelSavedStateAndroidTest {

  @Test
  fun testSavedStateRoundtripAndroidRobolectric() =
    MeNavViewModelSavedStateTestHelper.assertMeNavViewModelSavedStateRoundtrip()
}
