package mikufan.cx.conduit.frontend.logic.component.main.me

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Shared test logic for [MeNavViewModel] SavedState roundtrip verification.
 * Target-specific test files invoke these helper methods and return their [TestResult].
 */
@OptIn(ExperimentalCoroutinesApi::class)
object MeNavViewModelSavedStateTestHelper {

  fun assertMeNavViewModelSavedStateRoundtrip(): TestResult {
    val testDispatcher = StandardTestDispatcher()
    return runTest(testDispatcher) {
      Dispatchers.setMain(testDispatcher)
      val ownedStores = mutableListOf<ViewModelStore>()
      try {
        val storeFactory = MeNavStoreFactory(DefaultStoreFactory(), testDispatcher)
        val vmFactory = MeNavViewModelFactory(storeFactory)

        val vmStore1 = ViewModelStore()
        ownedStores += vmStore1
        val handle1 = SavedStateHandle()
        val vm1 = ViewModelProvider.create(
          vmStore1,
          viewModelFactory { addInitializer(MeNavViewModel::class) { vmFactory.create(handle1) } }
        )[MeNavViewModel::class]

        testScheduler.runCurrent()
        assertEquals(1, vm1.state.value.stack.size)

        // Navigate to EditProfile
        val loadedMe = LoadedMe(email = "alice@test.com", username = "alice", bio = "my bio", imageUrl = "alice.png")
        vm1.onEditProfile(loadedMe)
        testScheduler.runCurrent()

        assertEquals(2, vm1.state.value.stack.size)
        val editRoute1 = vm1.state.value.currentRoute as MeNavRoute.EditProfile
        assertEquals("edit_1", editRoute1.entryId)
        assertEquals(loadedMe, editRoute1.loadedMe)
        assertEquals(2L, vm1.state.value.nextEntryId)

        // Snapshot state
        val savedState = handle1.savedStateProvider().saveState()

        // Clear vmStore1
        vmStore1.clear()
        assertTrue(vm1.isStoreDisposed)

        // Restore into fresh handle and vmStore2
        val vmStore2 = ViewModelStore()
        ownedStores += vmStore2
        val handle2 = SavedStateHandle.createHandle(savedState, null)
        val vm2 = ViewModelProvider.create(
          vmStore2,
          viewModelFactory { addInitializer(MeNavViewModel::class) { vmFactory.create(handle2) } }
        )[MeNavViewModel::class]

        testScheduler.runCurrent()
        assertEquals(2, vm2.state.value.stack.size)
        val restoredTop = vm2.state.value.currentRoute as MeNavRoute.EditProfile
        assertEquals("edit_1", restoredTop.entryId)
        assertEquals(loadedMe, restoredTop.loadedMe)
        assertEquals(2L, vm2.state.value.nextEntryId)

        // Verify pop on restored VM
        vm2.onPopRoute("edit_1")
        testScheduler.runCurrent()
        assertEquals(1, vm2.state.value.stack.size)
        assertTrue(vm2.state.value.currentRoute is MeNavRoute.Profile)
      } finally {
        ownedStores.forEach { it.clear() }
        Dispatchers.resetMain()
      }
    }
  }
}
