package mikufan.cx.conduit.frontend.logic.component.main.feed

import kotlin.test.Test

class ArticlesNavViewModelSavedStateWasmTest {

  @Test
  fun testSavedStateRoundtripWasm() =
    ArticlesNavViewModelSavedStateTestHelper.assertArticlesNavViewModelSavedStateRoundtrip()
}
