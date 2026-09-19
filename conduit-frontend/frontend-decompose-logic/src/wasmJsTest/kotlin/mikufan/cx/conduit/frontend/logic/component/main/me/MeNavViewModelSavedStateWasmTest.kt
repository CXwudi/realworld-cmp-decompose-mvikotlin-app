package mikufan.cx.conduit.frontend.logic.component.main.me

import kotlin.test.Test

class MeNavViewModelSavedStateWasmTest {

  @Test
  fun testSavedStateRoundtripWasm() =
    MeNavViewModelSavedStateTestHelper.assertMeNavViewModelSavedStateRoundtrip()
}
