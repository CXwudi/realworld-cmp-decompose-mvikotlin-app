package mikufan.cx.conduit.frontend.logic.component.main

import kotlin.test.Test

/**
 * JS test wrapper for native SavedState roundtrip.
 */
class MainNavViewModelSavedStateJsTest {

  @Test
  fun testSavedStateRoundtripJs() =
    MainNavViewModelSavedStateTestHelper.assertMainNavViewModelSavedStateRoundtrip()

  @Test
  fun testPendingCandidatePreservedBeforeReadinessJs() =
    MainNavViewModelSavedStateTestHelper.assertPendingCandidatePreservedBeforeReadiness()
}
