package mikufan.cx.conduit.frontend.logic.component.main.me

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mikufan.cx.conduit.frontend.logic.component.util.rethrowIfShouldNotBeHandled
import mikufan.cx.conduit.frontend.logic.service.main.AddArticleService

class AddArticleStoreFactory(
  private val storeFactory: StoreFactory,
  private val addArticleService: AddArticleService,
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

  private val executor = coroutineExecutorFactory<AddArticleIntent, Nothing, AddArticleState, Msg, AddArticleLabel>(mainDispatcher) {
    onIntent<AddArticleIntent.TitleChanged> {
      dispatch(Msg.TitleChanged(it.title))
    }

    onIntent<AddArticleIntent.DescriptionChanged> {
      dispatch(Msg.DescriptionChanged(it.description))
    }

    onIntent<AddArticleIntent.BodyChanged> {
      dispatch(Msg.BodyChanged(it.body))
    }

    onIntent<AddArticleIntent.TagListChanged> {
      val splitTags = it.tagRawString.split(",", " ")
      dispatch(Msg.TagListChanged(splitTags))
    }

    onIntent<AddArticleIntent.Publish> {
      launch {
        try {
          val state = state()
          addArticleService.createArticle(
            title = state.title,
            description = state.description,
            body = state.body,
            tagList = state.tagList
          )
          publish(AddArticleLabel.PublishSuccess)
        } catch (e: Throwable) {
          rethrowIfShouldNotBeHandled(e) {
            log.error(e) { "Failed to publish article" }
            dispatch(Msg.ShowErrorMsg(e.message ?: "Unknown error"))
          }
        }
      }
    }

    onIntent<AddArticleIntent.BackWithoutPublish> {
      publish(AddArticleLabel.BackWithoutPublish)
    }
  }

  private val reducer = Reducer<AddArticleState, Msg> { msg ->
    when (msg) {
      is Msg.TitleChanged -> this.copy(title = msg.title, errorMsg = "")
      is Msg.DescriptionChanged -> this.copy(description = msg.description, errorMsg = "")
      is Msg.BodyChanged -> this.copy(body = msg.body, errorMsg = "")
      is Msg.TagListChanged -> this.copy(tagList = msg.tagList, errorMsg = "")
      is Msg.ShowErrorMsg -> this.copy(errorMsg = msg.errorMsg)
    }
  }

  fun createStore() = storeFactory.create(
    name = "AddArticleStore",
    initialState = AddArticleState(),
    executorFactory = executor,
    reducer = reducer
  )

  private sealed interface Msg {
    data class TitleChanged(val title: String) : Msg
    data class DescriptionChanged(val description: String) : Msg
    data class BodyChanged(val body: String) : Msg
    data class TagListChanged(val tagList: List<String>) : Msg
    data class ShowErrorMsg(val errorMsg: String) : Msg
  }
}

private val log = KotlinLogging.logger {}
