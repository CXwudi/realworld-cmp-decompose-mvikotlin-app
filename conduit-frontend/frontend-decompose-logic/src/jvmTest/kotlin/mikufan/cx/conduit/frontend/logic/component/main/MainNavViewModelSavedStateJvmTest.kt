package mikufan.cx.conduit.frontend.logic.component.main

import kotlin.test.Test

/**
 * JVM test wrapper for native SavedState roundtrip.
 */
class MainNavViewModelSavedStateJvmTest {

  @Test
  fun testSavedStateRoundtripJvm() =
    MainNavViewModelSavedStateTestHelper.assertMainNavViewModelSavedStateRoundtrip()

  @Test
  fun testPendingCandidatePreservedBeforeReadinessJvm() =
    MainNavViewModelSavedStateTestHelper.assertPendingCandidatePreservedBeforeReadiness()
}
