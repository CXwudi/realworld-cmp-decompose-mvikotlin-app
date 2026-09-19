package mikufan.cx.conduit.frontend.logic.component.main.feed

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ArticlesNavViewModelSavedStateAndroidTest {

  @Test
  fun testSavedStateRoundtripAndroidRobolectric() =
    ArticlesNavViewModelSavedStateTestHelper.assertArticlesNavViewModelSavedStateRoundtrip()
}
