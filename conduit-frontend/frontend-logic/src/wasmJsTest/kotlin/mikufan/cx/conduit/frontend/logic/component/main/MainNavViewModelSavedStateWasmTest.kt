package mikufan.cx.conduit.frontend.logic.component.main

import kotlin.test.Test

/**
 * WasmJs test wrapper for native SavedState roundtrip.
 */
class MainNavViewModelSavedStateWasmTest {

  @Test
  fun testSavedStateRoundtripWasm() =
    MainNavViewModelSavedStateTestHelper.assertMainNavViewModelSavedStateRoundtrip()

  @Test
  fun testPendingCandidatePreservedBeforeReadinessWasm() =
    MainNavViewModelSavedStateTestHelper.assertPendingCandidatePreservedBeforeReadiness()
}
