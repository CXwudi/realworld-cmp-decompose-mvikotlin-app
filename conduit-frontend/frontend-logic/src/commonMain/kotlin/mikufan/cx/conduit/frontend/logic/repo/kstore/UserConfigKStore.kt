package mikufan.cx.conduit.frontend.logic.repo.kstore

import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


interface UserConfigKStore {
  /**
   * The default implementation uses a state flow.
   */
  val userConfigFlow: Flow<UserConfigState>

  suspend fun setUrl(url: String? = null)
  suspend fun setUserInfo(userInfo: UserInfo? = null)

  suspend fun set(newConfig: PersistedConfig)
  suspend fun reset()
}

sealed interface UserConfigState {
  data object Landing : UserConfigState
  data class OnUrl(val url: String) : UserConfigState
  data class OnLogin(val url: String, val userInfo: UserInfo) : UserConfigState
}

class UserConfigKStoreImpl(
  private val kStore: KStore<PersistedConfig>,
) : UserConfigKStore {
  /**
   * Note: under the hood, this is a state flow
   */
  override val userConfigFlow: Flow<UserConfigState> =
    kStore.updates.map { config ->
      when {
        config == null -> UserConfigState.Landing
        config.url != null && config.userInfo != null -> UserConfigState.OnLogin(config.url, config.userInfo)
        config.url != null -> UserConfigState.OnUrl(config.url)
        config.userInfo == null -> UserConfigState.Landing // if both are null
        else -> throw IllegalArgumentException("Invalid user config state: $config")
      }
    }

  override suspend fun setUrl(url: String?) = kStore.update {
    if (url == "") throw IllegalArgumentException("url cannot be empty")
    it?.copy(url = url)
  }

  override suspend fun setUserInfo(userInfo: UserInfo?) = kStore.update {
    it?.copy(userInfo = userInfo)
  }

  override suspend fun set(newConfig: PersistedConfig) = kStore.set(newConfig)

  override suspend fun reset() = set(PersistedConfig())

}