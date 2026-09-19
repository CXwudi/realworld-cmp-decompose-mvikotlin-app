package mikufan.cx.conduit.frontend.logic.service.main

import mikufan.cx.conduit.common.UserReq
import mikufan.cx.conduit.common.UserUpdateDto
import mikufan.cx.conduit.frontend.logic.component.main.me.LoadedMe
import mikufan.cx.conduit.frontend.logic.repo.api.AuthApi
import mikufan.cx.conduit.frontend.logic.repo.api.util.getOrThrow
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserInfo


interface EditProfileService {
  suspend fun updateAndSave(profileUpdate: UserUpdateDto)
}

class DefaultEditProfileService(
  private val authApi: AuthApi,
  private val userConfigKStore: UserConfigKStore,
) : EditProfileService {

  override suspend fun updateAndSave(profileUpdate: UserUpdateDto) {
    val rsp = authApi.updateCurrentUser(UserReq(profileUpdate))
    val newUser = rsp.getOrThrow().user

    // Update kstore with new user info
    val updatedUserInfo = UserInfo(
      email = newUser.email,
      username = newUser.username,
      bio = newUser.bio,
      image = newUser.image,
      token = newUser.token
    )
    userConfigKStore.setUserInfo(updatedUserInfo)
  }
}