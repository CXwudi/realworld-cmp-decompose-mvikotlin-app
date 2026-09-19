package mikufan.cx.conduit.frontend.logic.component.main.me

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
import mikufan.cx.conduit.frontend.logic.service.main.MePageService
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState

class MeStoreFactory(
  private val storeFactory: StoreFactory,
  private val mePageService: MePageService,
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

  private val executorFactory = coroutineExecutorFactory<MePageIntent, Action, MePageState, Msg, MePageLabel>(mainDispatcher) {
    onAction<Action.LoadMeSuccess> {
      val loadedMe = it.loadedMe
      dispatch(Msg.LoadMe(loadedMe))
    }

    onAction<Action.LoadMeError> {
      val errorMsg = it.errorMsg
      dispatch(Msg.LoadMeError(errorMsg))
    }

    onIntent<MePageIntent.Logout> {
      launch {
        try {
          withContext(Dispatchers.Default) {
            mePageService.logout()
          }
          publish(MePageLabel.TestOnly)
        } catch (t: Throwable) {
          rethrowIfShouldNotBeHandled(t) { e ->
            log.error(e) { "Failed to logout" }
          }
        }
      }

    }

    onIntent<MePageIntent.SwitchServer> {
      launch {
        try {
          withContext(Dispatchers.Default) {
            mePageService.switchServer()
          }
          publish(MePageLabel.TestOnly)
        } catch (t: Throwable) {
          rethrowIfShouldNotBeHandled(t) { e ->
            log.error(e) { "Failed to switch server" }
          }
        }
      }
    }

    onIntent<MePageIntent.EditProfile> {
      val currentState = state()
      if (currentState is MePageState.Loaded) {
        val loadedMe = LoadedMe(
          email = currentState.email,
          username = currentState.username,
          bio = currentState.bio,
          imageUrl = currentState.imageUrl
        )
        publish(MePageLabel.EditProfile(loadedMe))
      }
    }

    onIntent<MePageIntent.AddArticle> {
      publish(MePageLabel.AddArticle)
    }
  }

  private fun createBootstrapper() = coroutineBootstrapper(mainDispatcher) {
    launch {
      mePageService.userConfigFlow.collect { userConfigState ->
        when (userConfigState) {
          is UserConfigState.OnLogin -> {
            val userInfo = userConfigState.userInfo
            val loadedMe = LoadedMe(
              email = userInfo.email,
              username = userInfo.username,
              bio = userInfo.bio ?: "",
              imageUrl = userInfo.image ?: ""
            )
            dispatch(Action.LoadMeSuccess(loadedMe))
          }
          is UserConfigState.Landing, is UserConfigState.OnUrl -> {
            dispatch(Action.LoadMeError("User not logged in"))
          }
        }
      }
    }
  }

  private val reducer = Reducer<MePageState, Msg> { msg ->
    when (msg) {
      is Msg.LoadMe -> MePageState.Loaded(
        email = msg.loadedMe.email,
        imageUrl = msg.loadedMe.imageUrl,
        username = msg.loadedMe.username,
        bio = msg.loadedMe.bio,
      )
      is Msg.LoadMeError -> MePageState.Error(msg.errorMsg)
    }
  }

  fun createStore(autoInit: Boolean = true): Store<MePageIntent, MePageState, MePageLabel> {
    return storeFactory.create(
      name = "MePageStore",
      autoInit = autoInit,
      initialState = MePageState.Loading,
      executorFactory = executorFactory,
      bootstrapper = createBootstrapper(),
      reducer = reducer
    )
  }

  private sealed interface Action{
    data class LoadMeSuccess(val loadedMe: LoadedMe) : Action
    data class LoadMeError(val errorMsg: String) : Action
  }

  private sealed interface Msg {
    data class LoadMe(val loadedMe: LoadedMe) : Msg
    data class LoadMeError(val errorMsg: String) : Msg
  }
}

private val log = KotlinLogging.logger {}
