package mikufan.cx.conduit.frontend.logic.component.main

import com.arkivanov.mvikotlin.core.store.Bootstrapper
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState

class MainNavStoreFactory(
  private val storeFactory: StoreFactory,
  private val userConfigKStore: UserConfigKStore,
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

  private val executor =
    coroutineExecutorFactory<MainNavIntent, Action, MainNavState, Msg, Nothing>(dispatcher) {
      onAction<Action> { action ->
        val currentState = state()
        
        // Validate state consistency
        if (currentState.isLoggedIn) {
          val favouriteItem = currentState.menuItems.find { it is MainNavMenuItem.Favourite } as? MainNavMenuItem.Favourite
          if (favouriteItem == null) {
            log.error { "Inconsistent state: isLoggedIn=true but no Favourite menu item found. MenuItems: ${currentState.menuItems}" }
            throw IllegalStateException("Inconsistent state: logged in state must have Favourite menu item")
          }
        }
        
        val shouldDispatch = when (action) {
          is Action.SwitchToNotLoggedIn -> currentState.isLoggedIn
          is Action.SwitchToLoggedIn -> !currentState.isLoggedIn || 
            (currentState.menuItems.find { it is MainNavMenuItem.Favourite } as? MainNavMenuItem.Favourite)?.username != action.username
        }
        if (shouldDispatch) {
          val msg = when (action) {
            is Action.SwitchToNotLoggedIn -> Msg.SwitchToNotLoggedIn
            is Action.SwitchToLoggedIn -> Msg.SwitchToLoggedIn(action.username)
          }
          log.info { "Switching main page state based on action: $action" }
          dispatch(msg)
        }
      }

      onIntent<MainNavIntent.MenuItemSwitching> { intent ->
        val newMenuItem = intent.targetMenuItem
        val currentState = state()
        val newIdx: Int = requireNotNull(currentState.indexOfMenuItem(newMenuItem)) {
          "MenuItem $newMenuItem not found in state with items: ${currentState.menuItems}. Current pageIndex: ${currentState.pageIndex}"
        }
        if (currentState.pageIndex != newIdx) {
          log.info { "Switching to page at index $newIdx" }
          dispatch(Msg.MenuIndexSwitching(newIdx))
        }
      }
    }

  private val reducer = Reducer<MainNavState, Msg> { msg ->
    when (msg) {
      is Msg.SwitchToNotLoggedIn -> MainNavState.notLoggedIn()
      is Msg.SwitchToLoggedIn -> MainNavState.loggedIn(msg.username)
      is Msg.MenuIndexSwitching -> with(pageIndex = msg.targetIndex)
    }
  }

  private fun createBootstrapper(): Bootstrapper<Action> =
    coroutineBootstrapper(dispatcher) {
      launch {
        userConfigKStore.userConfigFlow.collect { userConfigState ->
          val action = when (userConfigState) {
            is UserConfigState.Landing -> throw IllegalStateException("Should not be Landing state after coming to MainNav")
            is UserConfigState.OnUrl -> Action.SwitchToNotLoggedIn
            is UserConfigState.OnLogin -> Action.SwitchToLoggedIn(userConfigState.userInfo.username)
          }
          dispatch(action)
        }
      }
    }

  fun createStore() = storeFactory.create(
    name = "MainNavStore",
    initialState = MainNavState.notLoggedIn(),
    bootstrapper = createBootstrapper(),
    executorFactory = executor,
    reducer = reducer,
  )

  private sealed interface Action {
    data object SwitchToNotLoggedIn : Action
    data class SwitchToLoggedIn(val username: String) : Action
  }

  private sealed interface Msg {
    data object SwitchToNotLoggedIn: Msg
    data class SwitchToLoggedIn(val username: String): Msg
    data class MenuIndexSwitching(val targetIndex: Int): Msg
  }

}

private val log = KotlinLogging.logger { }