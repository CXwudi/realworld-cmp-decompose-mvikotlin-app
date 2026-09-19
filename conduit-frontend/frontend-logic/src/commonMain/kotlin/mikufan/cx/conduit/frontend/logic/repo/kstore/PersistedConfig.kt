package mikufan.cx.conduit.frontend.logic.repo.kstore

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
  val email: String,
  val username: String,
  val bio: String?,
  val image: String?,
  val token: String?
)

@Serializable
data class PersistedConfig(
  val url: String? = null,
  val userInfo: UserInfo? = null
)
