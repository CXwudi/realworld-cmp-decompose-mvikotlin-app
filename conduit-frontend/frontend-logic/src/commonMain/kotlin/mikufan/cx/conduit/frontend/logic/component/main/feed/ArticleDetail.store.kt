package mikufan.cx.conduit.frontend.logic.component.main.feed

import com.arkivanov.mvikotlin.core.store.Bootstrapper
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mikufan.cx.conduit.frontend.logic.component.util.rethrowIfShouldNotBeHandled
import mikufan.cx.conduit.frontend.logic.service.main.ArticleDetailService

class ArticleDetailStoreFactory(
  private val storeFactory: StoreFactory,
  private val articleDetailService: ArticleDetailService,
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

  private val executorFactory = coroutineExecutorFactory<ArticleDetailIntent, Action, ArticleDetailState, Msg, ArticleDetailLabel>(mainDispatcher) {
    onAction<Action.LoadArticleSuccess> {
      val detailInfo = it.articleDetailInfo
      dispatch(Msg.SetDetailInfo(detailInfo))
    }

    onAction<Action.LoadArticleError> {
      val errorMsg = it.errorMsg
      val exception = it.exception
      publish(ArticleDetailLabel.Failure(exception, errorMsg))
    }

    onIntent<ArticleDetailIntent.BackToList> {
      publish(ArticleDetailLabel.BackToList)
    }
  }

  private fun createBootstrapper(slug: String) = coroutineBootstrapper(mainDispatcher) {
    launch {
      try {
        val articleResponse = withContext(Dispatchers.Default) {
          articleDetailService.getArticle(slug)
        }
        dispatch(Action.LoadArticleSuccess(articleResponse))
      } catch (t: Throwable) {
        rethrowIfShouldNotBeHandled(t) { e ->
          log.error(e) { "Failed to load article: $slug" }
          dispatch(Action.LoadArticleError(e.message ?: "Failed to load article", e as? Exception))
        }
      }
    }
  }

  private val reducer = Reducer<ArticleDetailState, Msg> { msg ->
    when (msg) {
      is Msg.SetDetailInfo -> this.copy(detailInfo = msg.detailInfo)
    }
  }

  fun createStore(basicInfo: ArticleBasicInfo, autoInit: Boolean = true): Store<ArticleDetailIntent, ArticleDetailState, ArticleDetailLabel> {
    val initialState = ArticleDetailState(basicInfo = basicInfo)

    val bootstrapper: Bootstrapper<Action>? = if (autoInit) {
      createBootstrapper(basicInfo.slug)
    } else {
      null
    }

    return storeFactory.create(
      name = "ArticleDetailStore",
      autoInit = autoInit,
      initialState = initialState,
      executorFactory = executorFactory,
      bootstrapper = bootstrapper,
      reducer = reducer
    )
  }

  private sealed interface Action {
    data class LoadArticleSuccess(val articleDetailInfo: ArticleDetailInfo) : Action
    data class LoadArticleError(val errorMsg: String, val exception: Exception?) : Action
  }

  private sealed interface Msg {
    data class SetDetailInfo(val detailInfo: ArticleDetailInfo) : Msg
  }
}

private val log = KotlinLogging.logger {}
