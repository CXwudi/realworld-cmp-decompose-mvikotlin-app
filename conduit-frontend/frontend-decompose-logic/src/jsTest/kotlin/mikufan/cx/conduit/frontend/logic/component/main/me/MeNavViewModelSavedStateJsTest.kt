package mikufan.cx.conduit.frontend.logic.component.main.me

import kotlin.test.Test

class MeNavViewModelSavedStateJsTest {

  @Test
  fun testSavedStateRoundtripJs() =
    MeNavViewModelSavedStateTestHelper.assertMeNavViewModelSavedStateRoundtrip()
}
