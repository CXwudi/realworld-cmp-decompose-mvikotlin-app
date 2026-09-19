package mikufan.cx.conduit.frontend.logic.component.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Shared test logic for [MainNavViewModel] SavedState roundtrip verification.
 * Target-specific test files invoke these helper methods and return their [TestResult].
 */
@OptIn(ExperimentalCoroutinesApi::class)
object MainNavViewModelSavedStateTestHelper {

  fun assertMainNavViewModelSavedStateRoundtrip(): TestResult {
    val testDispatcher = StandardTestDispatcher()
    return runTest(testDispatcher) {
      Dispatchers.setMain(testDispatcher)
      val ownedStores = mutableListOf<ViewModelStore>()
      try {
        val userConfigFlow = MutableStateFlow<UserConfigState>(UserConfigState.OnUrl("https://example.com"))
        val userConfigKStore: UserConfigKStore = mock()
        every { userConfigKStore.userConfigFlow } returns userConfigFlow

        val storeFactory = MainNavStoreFactory(DefaultStoreFactory(), userConfigKStore, testDispatcher)
        val vmFactory = MainNavViewModelFactory(storeFactory)

        val userInfo = UserInfo("alice@test.com", "alice", null, null, "token")
        userConfigFlow.value = UserConfigState.OnLogin("https://example.com", userInfo)

        val vmStore1 = ViewModelStore()
        ownedStores += vmStore1
        val handle1 = SavedStateHandle()
        val vm1 = ViewModelProvider.create(
          vmStore1,
          viewModelFactory { addInitializer(MainNavViewModel::class) { vmFactory.create(handle1) } }
        )[MainNavViewModel::class]

        testScheduler.runCurrent()
        assertTrue(vm1.state.value.isReady)

        // Switch tab to Me (index 2)
        vm1.send(MainNavIntent.MenuIndexSwitching(2))
        testScheduler.runCurrent()
        assertEquals(2, vm1.state.value.pageIndex)
        val generation1 = vm1.state.value.generation

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
          viewModelFactory { addInitializer(MainNavViewModel::class) { vmFactory.create(handle2) } }
        )[MainNavViewModel::class]

        testScheduler.runCurrent()
        assertTrue(vm2.state.value.isReady)
        assertEquals(2, vm2.state.value.pageIndex)
        assertEquals(MainNavMenuItem.Me, vm2.state.value.currentMenuItem)
        assertEquals(generation1, vm2.state.value.generation)
      } finally {
        ownedStores.forEach { it.clear() }
        Dispatchers.resetMain()
      }
    }
  }

  fun assertPendingCandidatePreservedBeforeReadiness(): TestResult {
    val testDispatcher = StandardTestDispatcher()
    return runTest(testDispatcher) {
      Dispatchers.setMain(testDispatcher)
      val ownedStores = mutableListOf<ViewModelStore>()
      try {
        val userInfo = UserInfo("alice@test.com", "alice", null, null, "token")
        val userConfigFlow1 = MutableStateFlow<UserConfigState>(UserConfigState.OnLogin("https://example.com", userInfo))
        val userConfigKStore1: UserConfigKStore = mock()
        every { userConfigKStore1.userConfigFlow } returns userConfigFlow1

        val storeFactory1 = MainNavStoreFactory(DefaultStoreFactory(), userConfigKStore1, testDispatcher)
        val vmFactory1 = MainNavViewModelFactory(storeFactory1)

        // 1. Produce real saved tab + generation in vm1
        val vmStore1 = ViewModelStore()
        ownedStores += vmStore1
        val handle1 = SavedStateHandle()
        val vm1 = ViewModelProvider.create(
          vmStore1,
          viewModelFactory { addInitializer(MainNavViewModel::class) { vmFactory1.create(handle1) } }
        )[MainNavViewModel::class]
        testScheduler.runCurrent()

        vm1.send(MainNavIntent.MenuIndexSwitching(2))
        testScheduler.runCurrent()
        val originalGeneration = vm1.state.value.generation
        assertEquals(2, vm1.state.value.pageIndex)

        val savedState1 = handle1.savedStateProvider().saveState()
        vmStore1.clear()

        // 2. Restore into vm2 backed by a non-emitting Flow
        val nonEmittingFlow = Channel<UserConfigState>().receiveAsFlow()
        val userConfigKStore2: UserConfigKStore = mock()
        every { userConfigKStore2.userConfigFlow } returns nonEmittingFlow

        val storeFactory2 = MainNavStoreFactory(DefaultStoreFactory(), userConfigKStore2, testDispatcher)
        val vmFactory2 = MainNavViewModelFactory(storeFactory2)

        val vmStore2 = ViewModelStore()
        ownedStores += vmStore2
        val handle2 = SavedStateHandle.createHandle(savedState1, null)
        val vm2 = ViewModelProvider.create(
          vmStore2,
          viewModelFactory { addInitializer(MainNavViewModel::class) { vmFactory2.create(handle2) } }
        )[MainNavViewModel::class]
        testScheduler.runCurrent()

        // vm2 is not ready because flow did not emit
        assertFalse(vm2.state.value.isReady)

        // 3. Snapshot again while still unready
        val savedState2 = handle2.savedStateProvider().saveState()
        vmStore2.clear()

        // 4. Restore into vm3 backed by authoritative matching user
        val userConfigFlow3 = MutableStateFlow<UserConfigState>(UserConfigState.OnLogin("https://example.com", userInfo))
        val userConfigKStore3: UserConfigKStore = mock()
        every { userConfigKStore3.userConfigFlow } returns userConfigFlow3

        val storeFactory3 = MainNavStoreFactory(DefaultStoreFactory(), userConfigKStore3, testDispatcher)
        val vmFactory3 = MainNavViewModelFactory(storeFactory3)

        val vmStore3 = ViewModelStore()
        ownedStores += vmStore3
        val handle3 = SavedStateHandle.createHandle(savedState2, null)
        val vm3 = ViewModelProvider.create(
          vmStore3,
          viewModelFactory { addInitializer(MainNavViewModel::class) { vmFactory3.create(handle3) } }
        )[MainNavViewModel::class]
        testScheduler.runCurrent()

        // 5. Assert exact original tab and generation preserved
        assertTrue(vm3.state.value.isReady)
        assertEquals(2, vm3.state.value.pageIndex)
        assertEquals(MainNavMenuItem.Me, vm3.state.value.currentMenuItem)
        assertEquals(originalGeneration, vm3.state.value.generation)
      } finally {
        ownedStores.forEach { it.clear() }
        Dispatchers.resetMain()
      }
    }
  }
}
