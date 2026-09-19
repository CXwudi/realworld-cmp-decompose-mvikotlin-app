package mikufan.cx.conduit.frontend.logic.component.main.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mikufan.cx.conduit.frontend.logic.service.main.AuthService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var authService: AuthService
  private lateinit var storeFactory: AuthPageStoreFactory
  private lateinit var viewModelFactory: AuthViewModelFactory
  private lateinit var viewModelStore: ViewModelStore

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    authService = mock()
    storeFactory = AuthPageStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      authService,
      testDispatcher,
    )
    viewModelFactory = AuthViewModelFactory(storeFactory)
    viewModelStore = ViewModelStore()
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun testStoreDisposedOnViewModelClear() {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(AuthViewModel::class) {
          viewModelFactory.create()
        }
      }
    )
    val vm = provider[AuthViewModel::class]
    assertFalse(vm.isStoreDisposed)

    viewModelStore.clear()
    assertTrue(vm.isStoreDisposed)
  }

  @Test
  fun testStateAndIntentForwarding() = runTest(testDispatcher) {
    val provider = ViewModelProvider.create(
      viewModelStore,
      viewModelFactory {
        addInitializer(AuthViewModel::class) {
          viewModelFactory.create()
        }
      }
    )
    val vm = provider[AuthViewModel::class]

    assertEquals("", vm.state.value.email)
    assertEquals(AuthPageMode.SIGN_IN, vm.state.value.mode)

    vm.send(AuthPageIntent.EmailChanged("test@example.com"))
    vm.send(AuthPageIntent.PasswordChanged("secret123"))
    testScheduler.runCurrent()

    assertEquals("test@example.com", vm.state.value.email)
    assertEquals("secret123", vm.state.value.password)

    // SwitchMode clears email and password and toggles mode
    vm.send(AuthPageIntent.SwitchMode)
    testScheduler.runCurrent()

    assertEquals("", vm.state.value.email)
    assertEquals("", vm.state.value.password)
    assertEquals(AuthPageMode.REGISTER, vm.state.value.mode)

    vm.send(AuthPageIntent.UsernameChanged("alice"))
    testScheduler.runCurrent()
    assertEquals("alice", vm.state.value.username)
  }
}
