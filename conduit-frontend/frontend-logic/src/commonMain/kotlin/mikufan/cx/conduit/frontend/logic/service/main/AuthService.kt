package mikufan.cx.conduit.frontend.logic.service.main

import mikufan.cx.conduit.common.UserDtoUtils
import mikufan.cx.conduit.frontend.logic.repo.api.AuthApi
import mikufan.cx.conduit.frontend.logic.repo.api.util.getOrThrow
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserInfo

interface AuthService {
  suspend fun login(email: String, password: String)
  suspend fun register(email: String, username: String, password: String)
  suspend fun reset()
}

class DefaultAuthService(
  private val authApi: AuthApi,
  private val userConfigKStore: UserConfigKStore,
) : AuthService {

  override suspend fun login(email: String, password: String) {
    val req = UserDtoUtils.createLoginReq(email, password)
    val rsp = authApi.login(req)
    val userRsp = rsp.getOrThrow()
    val userDto = userRsp.user
    val userInfo = UserInfo(
      email = userDto.email,
      username = userDto.username,
      bio = userDto.bio,
      image = userDto.image,
      token = userDto.token
    )
    if (userInfo.token.isNullOrBlank()) error("Token is null or blank")
    userConfigKStore.setUserInfo(userInfo)
  }

  override suspend fun register(email: String, username: String, password: String) {
    val req = UserDtoUtils.createRegisterReq(email, username, password)
    val rsp = authApi.register(req)
    val userRsp = rsp.getOrThrow()
    val userDto = userRsp.user
    val userInfo = UserInfo(
      email = userDto.email,
      username = userDto.username,
      bio = userDto.bio,
      image = userDto.image,
      token = userDto.token
    )
    if (userInfo.token.isNullOrBlank()) error("Token is null or blank")
    userConfigKStore.setUserInfo(userInfo)
  }

  override suspend fun reset() {
    userConfigKStore.reset()
  }


}