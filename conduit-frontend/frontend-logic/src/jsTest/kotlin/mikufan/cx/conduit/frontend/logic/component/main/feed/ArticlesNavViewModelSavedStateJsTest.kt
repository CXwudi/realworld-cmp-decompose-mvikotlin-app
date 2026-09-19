package mikufan.cx.conduit.frontend.logic.component.main.feed

import kotlin.test.Test

class ArticlesNavViewModelSavedStateJsTest {

  @Test
  fun testSavedStateRoundtripJs() =
    ArticlesNavViewModelSavedStateTestHelper.assertArticlesNavViewModelSavedStateRoundtrip()
}
