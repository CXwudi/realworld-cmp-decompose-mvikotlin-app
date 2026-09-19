package mikufan.cx.conduit.frontend.logic.component.landing

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mikufan.cx.conduit.frontend.logic.component.util.rethrowIfShouldNotBeHandled
import mikufan.cx.conduit.frontend.logic.service.landing.LandingService


class LandingPageStoreFactory(
  private val storeFactory: StoreFactory,
  private val landingService: LandingService,
  mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
  defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

  internal sealed interface Msg {
    data class TextChanged(val text: String) : Msg
  }

  private val executor =
    coroutineExecutorFactory<LandingPageIntent, Nothing, LandingPageState, Msg, LandingPageLabel>(
      mainDispatcher
    ) {
      onIntent<LandingPageIntent.TextChanged> {
        dispatch(Msg.TextChanged(it.text))
      }
      onIntent<LandingPageIntent.CheckAndMoveToMainPage> {

        launch {
          try {
            val url = state().url
            log.info { "Checking accessibility for $url" }
            withContext(defaultDispatcher) {
              landingService.checkAccessibilityAndSetUrl(url)
            }
            log.debug { "Set url = $url" }
            publish(LandingPageLabel.ToNextPage)
          } catch (e: Throwable) {
            rethrowIfShouldNotBeHandled(e) {
              log.debug { "Failed with exception $e" }
              publish(LandingPageLabel.Failure(e.message ?: "Unknown error happened while checking URL accessibility"))
            }
          }
        }

      }
    }

  private val reducer = Reducer<LandingPageState, Msg> { msg ->
    when (msg) {
      is Msg.TextChanged -> copy(url = msg.text)
    }
  }

  fun createStore() = storeFactory.create(
    name = "LandingPageStore",
    initialState = LandingPageState(""),
    executorFactory = executor,
    reducer = reducer
  )
}

private val log = KotlinLogging.logger { }

