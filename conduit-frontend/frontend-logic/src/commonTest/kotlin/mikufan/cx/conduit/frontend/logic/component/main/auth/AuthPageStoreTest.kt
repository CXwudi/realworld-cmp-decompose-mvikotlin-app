package mikufan.cx.conduit.frontend.logic.component.main.auth

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.labelsChannel
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mikufan.cx.conduit.frontend.logic.service.main.AuthService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthPageStoreTest {
  private val testDispatcher = StandardTestDispatcher()

  lateinit var authService: AuthService
  lateinit var authPageStore: Store<AuthPageIntent, AuthPageState, AuthPageLabel>

  @BeforeTest
  fun setUp() {
    authService = mock()
    authPageStore = AuthPageStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      authService,
      testDispatcher,
    ).createStore()
  }

  @AfterTest
  fun reset() {
    authPageStore.dispose()
  }

  @Test
  fun normalFlow1() {
    authPageStore.accept(AuthPageIntent.UsernameChanged("new username"))
    assertEquals(authPageStore.state.username, "new username")
    authPageStore.accept(AuthPageIntent.PasswordChanged("new password"))
    assertEquals(authPageStore.state.password, "new password")

    authPageStore.accept(AuthPageIntent.SwitchMode)
    assertEquals(authPageStore.state.mode, AuthPageMode.REGISTER)
  }

  @Test
  fun backToLanding() = runTest(testDispatcher) {
    val testScope = TestScope()
    val channel = authPageStore.labelsChannel(testScope) // label channel in another scope

    authPageStore.accept(AuthPageIntent.BackToLanding)

    assertEquals(AuthPageLabel.BackToLanding, channel.receive())
    verifySuspend(exactly(1)) { authService.reset() }
    testScope.cancel()
  }
}