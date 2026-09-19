package mikufan.cx.conduit.frontend.logic.component.main.me

import kotlin.test.Test

class MeNavViewModelSavedStateJvmTest {

  @Test
  fun testSavedStateRoundtripJvm() =
    MeNavViewModelSavedStateTestHelper.assertMeNavViewModelSavedStateRoundtrip()
}
