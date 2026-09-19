package mikufan.cx.conduit.frontend.logic.component.landing

import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.ExperimentalMviKotlinApi
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.labelsChannel
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mikufan.cx.conduit.frontend.logic.service.landing.LandingService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LandingPageStoreTest {

  private val testDispatcher = StandardTestDispatcher()

  lateinit var landingService: LandingService
  lateinit var landingPageStore: Store<LandingPageIntent, LandingPageState, LandingPageLabel>

  @BeforeTest
  fun setUp() {
    landingService = mock()
    landingPageStore = LandingPageStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      landingService,
      testDispatcher
    ).createStore()
  }

  @AfterTest
  fun reset() {
    landingPageStore.dispose()
  }

  @Test
  fun testNormalFlowWithManualChannel() = runTest(testDispatcher) {
    val channel = Channel<LandingPageLabel>()

    landingPageStore.labels(observer(
      onComplete = { channel.close() },
      onNext = {
        this.launch {
          channel.send(it)
          log.debug { "Received $it" }
        }
      }
    ))

    landingPageStore.accept(LandingPageIntent.TextChanged("a change"))
    assertEquals("a change", landingPageStore.state.url)
    landingPageStore.accept(LandingPageIntent.CheckAndMoveToMainPage)

    val label = channel.receive()
    assertEquals(label, LandingPageLabel.ToNextPage)
    verifySuspend(exactly(1)) {
      landingService.checkAccessibilityAndSetUrl("a change")
    }
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testNormalFlowWithLabelChannelAndSeparateTestScope() = runTest(testDispatcher) {
    // the labelsChannel internally will create a coroutine and just wait for cancellation
    // hence using separate scope for label channel to unblock the runTest
    // otherwise deadlock as the runTest is waiting for the labelsChannel to be closed,
    // but the labelsChannel is waiting for the runTest to close the scope
    val separateScope = TestScope(testDispatcher)
    val labelsChannel = landingPageStore.labelsChannel(separateScope)

    landingPageStore.accept(LandingPageIntent.TextChanged("a change"))
    assertEquals("a change", landingPageStore.state.url)
    landingPageStore.accept(LandingPageIntent.CheckAndMoveToMainPage)

    val label = labelsChannel.receive()
    log.debug { "Received label" }
    assertEquals(label, LandingPageLabel.ToNextPage)
    verifySuspend(exactly(1)) {
      landingService.checkAccessibilityAndSetUrl("a change")
    }
    log.debug { "After verify" }
    separateScope.cancel()
//    labelsChannel.dispose() // optional you can do this instead of separateScope.cancel()
  }

  @Test
  fun testNormalFlowWithFlow() = runTest(testDispatcher) {
    // the labels Flow works and the label is actually dispatched.
    // however, the launched coroutine is stuck on the collect call, so the test will never finish
    // if the store.dispose() is not called before runTest finishes
    launch {
      landingPageStore.labels.collect {
        log.debug { "Received $it" }
        assertEquals(LandingPageLabel.ToNextPage, it)
        verifySuspend(exactly(1)) {
          landingService.checkAccessibilityAndSetUrl("a change")
        }
        log.debug { "Test finished successfully" }
      }
    }

    landingPageStore.accept(LandingPageIntent.TextChanged("a change"))
    assertEquals("a change", landingPageStore.state.url)
    landingPageStore.accept(LandingPageIntent.CheckAndMoveToMainPage)
    landingPageStore.dispose()
    // can't do cancel() as it will throw an exception
  }

  @Test
  fun testErrorFlow1() = runTest(testDispatcher) {
    everySuspend { landingService.checkAccessibilityAndSetUrl(any()) } throws Exception("some error")

    val stateChannel = Channel<LandingPageState>()
    landingPageStore.states(observer {
      this.launch {
        stateChannel.send(it)
        log.debug { "Received $it" }
      }
    })

    val testScope = TestScope(testDispatcher)
    val labelChannel = landingPageStore.labelsChannel(testScope)

    landingPageStore.accept(LandingPageIntent.CheckAndMoveToMainPage)

    stateChannel.receive() // ignore the first initial state
    val newLabel = labelChannel.receive()

    assertIs<LandingPageLabel.Failure>(newLabel)
    assertEquals("some error", newLabel.message)
    verifySuspend(exactly(1)) {
      landingService.checkAccessibilityAndSetUrl("")
    }
    stateChannel.close()
    testScope.cancel()
  }
}

private val log = KotlinLogging.logger { }