package mikufan.cx.conduit.frontend.logic.component.main.feed

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Factory creating the [Store] for Feed / Favourite list-detail navigation.
 *
 * Guarantees:
 * - Minimum stack size of 1 ([ArticlesNavRoute.List]). Root list is never popped.
 * - Maximum stack size of 2 ([ArticlesNavRoute.List] + [ArticlesNavRoute.Detail]).
 * - Selecting another article replaces any existing detail with a fresh unique [ArticlesNavRoute.Detail.entryId].
 * - Duplicate selection of the same article (matching slug) is a no-op that does not reset active detail.
 * - Stale [ArticlesNavIntent.CloseDetail] events from replaced/closed entries are rejected.
 * - Restored snapshots are guarded against invalid topologies, falling back cleanly to root list.
 */
class ArticlesNavStoreFactory(
  private val storeFactory: StoreFactory,
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

  private val reducer = Reducer<ArticlesNavState, Msg> { msg ->
    when (msg) {
      is Msg.UpdateStack -> copy(stack = msg.stack, nextEntryId = msg.nextEntryId)
    }
  }

  fun createStore(
    restoredSnapshot: ArticlesNavSavedSnapshot? = null,
  ): Store<ArticlesNavIntent, ArticlesNavState, Nothing> {
    val initialState = validateAndResolveInitialState(restoredSnapshot)

    val executorFactory = coroutineExecutorFactory<ArticlesNavIntent, Nothing, ArticlesNavState, Msg, Nothing>(
      mainContext = mainDispatcher,
    ) {
      onIntent<ArticlesNavIntent.OpenArticle> { intent ->
        val currentState = state()
        val currentRoute = currentState.currentRoute

        // Duplicate selection check: same article slug does not grow stack or reset detail
        if (currentRoute is ArticlesNavRoute.Detail && currentRoute.basicInfo.slug == intent.basicInfo.slug) {
          log.debug { "OpenArticle(${intent.basicInfo.slug}) ignored: article is already active" }
          return@onIntent
        }

        val entryId = "detail_${currentState.nextEntryId}"
        val newDetail = ArticlesNavRoute.Detail(basicInfo = intent.basicInfo, entryId = entryId)
        val listRoute = currentState.stack.firstOrNull() as? ArticlesNavRoute.List ?: ArticlesNavRoute.List()
        val newStack = listOf(listRoute, newDetail)

        dispatch(Msg.UpdateStack(stack = newStack, nextEntryId = currentState.nextEntryId + 1L))
      }

      onIntent<ArticlesNavIntent.CloseDetail> { intent ->
        val currentState = state()
        if (currentState.stack.size > 1 && currentState.stack.last().entryId == intent.entryId) {
          val listRoute = currentState.stack.first()
          dispatch(Msg.UpdateStack(stack = listOf(listRoute), nextEntryId = currentState.nextEntryId))
        } else {
          log.debug { "CloseDetail(${intent.entryId}) rejected: stale event or detail no longer active" }
        }
      }

      onIntent<ArticlesNavIntent.Pop> {
        val currentState = state()
        if (currentState.stack.size > 1) {
          val listRoute = currentState.stack.first()
          dispatch(Msg.UpdateStack(stack = listOf(listRoute), nextEntryId = currentState.nextEntryId))
        } else {
          log.debug { "Pop ignored: cannot pop root list" }
        }
      }
    }

    return storeFactory.create(
      name = "ArticlesNavStore",
      initialState = initialState,
      executorFactory = executorFactory,
      reducer = reducer,
    )
  }

  private fun validateAndResolveInitialState(snapshot: ArticlesNavSavedSnapshot?): ArticlesNavState {
    if (snapshot == null) {
      return ArticlesNavState()
    }

    val isValid = snapshot.stack.isNotEmpty() &&
        snapshot.stack.size in 1..2 &&
        snapshot.stack[0] is ArticlesNavRoute.List &&
        (snapshot.stack.size == 1 || snapshot.stack[1] is ArticlesNavRoute.Detail) &&
        snapshot.stack.map { it.entryId }.distinct().size == snapshot.stack.size &&
        snapshot.nextEntryId >= 1L

    return if (isValid) {
      ArticlesNavState(stack = snapshot.stack, nextEntryId = snapshot.nextEntryId)
    } else {
      log.warn { "Corrupted or incompatible ArticlesNav snapshot detected ($snapshot); falling back to root list" }
      ArticlesNavState()
    }
  }

  private sealed interface Msg {
    data class UpdateStack(
      val stack: List<ArticlesNavRoute>,
      val nextEntryId: Long,
    ) : Msg
  }
}

private val log = KotlinLogging.logger {}
