package mikufan.cx.conduit.frontend.logic.component.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState

/**
 * Root ViewModel driving root navigation between Loading, Landing, and Main(serverUrl).
 * Root gate is derived from [UserConfigKStore].
 *
 * Initial route is null (Loading outside NavDisplay) until KStore emits an authoritative gate.
 * Replacement navigation: does not accumulate root backstack history.
 * Main entry identity includes [RootRoute.Main.serverUrl] so that URL changes discard old subtree state,
 * while same-server login/profile transitions maintain identity and are reconciled inside MainNav.
 */
class RootViewModel(
  private val userConfigKStore: UserConfigKStore,
) : ViewModel() {

  private val _currentRoute = MutableStateFlow<RootRoute?>(null)
  val currentRoute: StateFlow<RootRoute?> = _currentRoute.asStateFlow()

  init {
    viewModelScope.launch {
      userConfigKStore.userConfigFlow
        .map { state ->
          when (state) {
            is UserConfigState.Landing -> RootRoute.Landing
            is UserConfigState.OnUrl -> RootRoute.Main(state.url)
            is UserConfigState.OnLogin -> RootRoute.Main(state.url)
          }
        }
        .collectLatest { targetRoute ->
          log.debug { "Root gate switching to $targetRoute" }
          _currentRoute.value = targetRoute
        }
    }
  }
}

/**
 * Plain assisted factory for creating [RootViewModel].
 * Stateless constructor receives services/stores; create creates fresh VM.
 */
class RootViewModelFactory(
  private val userConfigKStore: UserConfigKStore,
) {
  fun create(): RootViewModel = RootViewModel(userConfigKStore)
}

private val log = KotlinLogging.logger { }
