package mikufan.cx.conduit.frontend.logic.service.main

import kotlinx.coroutines.flow.Flow
import mikufan.cx.conduit.frontend.logic.component.main.me.LoadedMe
import mikufan.cx.conduit.frontend.logic.repo.api.AuthApi
import mikufan.cx.conduit.frontend.logic.repo.api.util.getOrThrow
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState

interface MePageService {
  val userConfigFlow: Flow<UserConfigState>
  suspend fun logout()
  suspend fun switchServer()
}

class DefaultMePageService(
  private val authApi: AuthApi,
  private val userConfigKStore: UserConfigKStore,
) : MePageService {

  override val userConfigFlow: Flow<UserConfigState> = userConfigKStore.userConfigFlow

  override suspend fun logout() {
    userConfigKStore.setUserInfo(null)
  }

  override suspend fun switchServer() {
    userConfigKStore.reset()
  }
}
