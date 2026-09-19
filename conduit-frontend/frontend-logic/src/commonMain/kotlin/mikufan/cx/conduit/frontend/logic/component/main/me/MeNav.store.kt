package mikufan.cx.conduit.frontend.logic.component.main.me

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Factory for creating stores managing the Me navigation backstack.
 */
class MeNavStoreFactory(
  private val storeFactory: StoreFactory,
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

  private val executorFactory = coroutineExecutorFactory<MeNavIntent, Nothing, MeNavState, Msg, Nothing>(mainDispatcher) {
    onIntent<MeNavIntent.NavigateToEditProfile> { intent ->
      val currentState = state()
      // Repeated click protection: only allow push when currently on Profile
      if (currentState.currentRoute is MeNavRoute.Profile) {
        val entryId = "edit_${currentState.nextEntryId}"
        val newRoute = MeNavRoute.EditProfile(intent.loadedMe, entryId)
        dispatch(Msg.Push(newRoute))
      }
    }

    onIntent<MeNavIntent.NavigateToAddArticle> {
      val currentState = state()
      // Repeated click protection: only allow push when currently on Profile
      if (currentState.currentRoute is MeNavRoute.Profile) {
        val entryId = "add_${currentState.nextEntryId}"
        val newRoute = MeNavRoute.AddArticle(entryId)
        dispatch(Msg.Push(newRoute))
      }
    }

    onIntent<MeNavIntent.PopRoute> { intent ->
      val currentState = state()
      // Reject stale or duplicate completion: entryId must match the top route
      if (currentState.stack.size > 1 && currentState.currentRoute.entryId == intent.entryId) {
        dispatch(Msg.Pop)
      }
    }

    onIntent<MeNavIntent.Pop> {
      val currentState = state()
      if (currentState.stack.size > 1) {
        dispatch(Msg.Pop)
      }
    }
  }

  private val reducer = Reducer<MeNavState, Msg> { msg ->
    when (msg) {
      is Msg.Push -> this.copy(
        stack = stack + msg.route,
        nextEntryId = nextEntryId + 1L,
      )
      is Msg.Pop -> if (stack.size > 1) {
        this.copy(stack = stack.dropLast(1))
      } else {
        this
      }
    }
  }

  private sealed interface Msg {
    data class Push(val route: MeNavRoute) : Msg
    data object Pop : Msg
  }

  fun createStore(
    restoredSnapshot: MeNavSavedSnapshot? = null,
    autoInit: Boolean = true,
  ): Store<MeNavIntent, MeNavState, Nothing> {
    val initialStack = if (restoredSnapshot != null && isValidTopology(restoredSnapshot.stack)) {
      restoredSnapshot.stack
    } else {
      listOf(MeNavRoute.Profile())
    }

    val initialNextId = restoredSnapshot?.nextEntryId ?: 1L

    return storeFactory.create(
      name = "MeNavStore",
      autoInit = autoInit,
      initialState = MeNavState(
        stack = initialStack,
        nextEntryId = initialNextId,
      ),
      executorFactory = executorFactory,
      reducer = reducer,
    )
  }

  private fun isValidTopology(stack: List<MeNavRoute>): Boolean {
    if (stack.size !in 1..2) return false
    if (stack.first() !is MeNavRoute.Profile) return false
    if (stack.size == 2) {
      val top = stack[1]
      if (top !is MeNavRoute.EditProfile && top !is MeNavRoute.AddArticle) return false
      if (top.entryId == stack[0].entryId) return false
    }
    return true
  }
}
