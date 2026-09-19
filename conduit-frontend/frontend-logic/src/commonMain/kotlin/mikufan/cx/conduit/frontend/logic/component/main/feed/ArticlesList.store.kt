package mikufan.cx.conduit.frontend.logic.component.main.feed

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutorScope
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mikufan.cx.conduit.frontend.logic.component.util.rethrowIfShouldNotBeHandled
import mikufan.cx.conduit.frontend.logic.service.main.ArticlesListService

class ArticlesListStoreFactory(
  private val storeFactory: StoreFactory,
  private val articlesListService: ArticlesListService,
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

  /**
   * Execute an operation in a coroutine with standardized error handling.
   *
   * @param block The operation to execute inside the try-catch block
   */
  private inline fun CoroutineExecutorScope<
      ArticlesListState, Msg, Nothing, ArticlesListLabel>.executeWithErrorHandling(
    crossinline execution: suspend () -> Unit,
    crossinline onFailure: (Throwable) -> Unit
  ) = launch {
    try {
      execution()
    } catch (t: Throwable) {
      rethrowIfShouldNotBeHandled(t) {
        onFailure(t)
      }
    }
  }


  private fun createExecutor(searchFilter: ArticlesSearchFilter) =
    coroutineExecutorFactory<ArticlesListIntent, Nothing, ArticlesListState, Msg, ArticlesListLabel>(
      mainContext = mainDispatcher
    ) {
      onIntent<ArticlesListIntent.LoadMore> {
        val state = state()
        // this dispatch is outside executeWithErrorHandling
        // because we want to set the state to LoadingMore immediately
        // so that when the UI finish calling state.send(intent),
        // the state is already LoadingMore
        dispatch(Msg.SetToLoadingMore)
        executeWithErrorHandling(
          execution = {
            val moreInfo = withContext(Dispatchers.Default) {
              articlesListService.getArticles(
                searchFilter,
                offset = state.collectedThumbInfos.size
              )
            }
            // TODO: need to add one more state called AllLoaded, means no more articles to load
            // this can be determined by checking the total count, or if the returned list is empty
            dispatch(Msg.AddMoreArticles(moreInfo))
          },
          onFailure = { t ->
            dispatch(Msg.LoadingMoreAborted)
            publish(
              ArticlesListLabel.Failure(
                exception = if (t is Exception) t else null,
                message = t.message ?: "Unknown error"
              )
            )
          }
        )
      }
      
      onIntent<ArticlesListIntent.ClickOnArticle> { intent ->
        publish(ArticlesListLabel.OpenArticle(intent.articleBasicInfo))
      }
    }

  private fun createReducer() = Reducer<ArticlesListState, Msg> { msg ->
    when (msg) {
      is Msg.SetToLoadingMore -> copy(loadMoreState = LoadMoreState.Loading)
      is Msg.AddMoreArticles -> copy(
        collectedThumbInfos = collectedThumbInfos + msg.articleInfos,
        loadMoreState = LoadMoreState.Loaded
      )
      is Msg.LoadingMoreAborted -> copy(loadMoreState = LoadMoreState.Loaded)
    }
  }

  fun create(searchFilter: ArticlesSearchFilter, autoInit: Boolean = true) =
    storeFactory.create(
      name = "ArticlesListStore-${searchFilter.serializedName}",
      initialState = ArticlesListState(),
      executorFactory = createExecutor(searchFilter),
      reducer = createReducer(),
      autoInit = autoInit
    )

  private sealed interface Msg {
    data object SetToLoadingMore : Msg
    data class AddMoreArticles(val articleInfos: List<ArticleInfo>) : Msg
    data object LoadingMoreAborted : Msg
  }
}

private val log = KotlinLogging.logger {}
