package mikufan.cx.conduit.frontend.logic.component.main.feed

import kotlin.test.Test

class ArticlesNavViewModelSavedStateJvmTest {

  @Test
  fun testSavedStateRoundtripJvm() =
    ArticlesNavViewModelSavedStateTestHelper.assertArticlesNavViewModelSavedStateRoundtrip()
}
