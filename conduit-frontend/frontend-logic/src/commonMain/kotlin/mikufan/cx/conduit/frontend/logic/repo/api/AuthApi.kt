package mikufan.cx.conduit.frontend.logic.repo.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import mikufan.cx.conduit.common.UserLoginDto
import mikufan.cx.conduit.common.UserRegisterDto
import mikufan.cx.conduit.common.UserReq
import mikufan.cx.conduit.common.UserRsp
import mikufan.cx.conduit.common.UserUpdateDto
import mikufan.cx.conduit.frontend.logic.repo.api.util.ConduitResponse


interface AuthApi {

  @POST("users")
  @Headers("Content-Type: application/json")
  suspend fun register(
    @Body body: UserReq<UserRegisterDto>
  ): ConduitResponse<UserRsp>

  @POST("users/login")
  @Headers("Content-Type: application/json")
  suspend fun login(
    @Body body: UserReq<UserLoginDto>
  ): ConduitResponse<UserRsp>

  @GET("user")
  @Headers("Content-Type: application/json")
  suspend fun getCurrentUser(): ConduitResponse<UserRsp>

  @PUT("user")
  @Headers("Content-Type: application/json")
  suspend fun updateCurrentUser(
    @Body body: UserReq<UserUpdateDto>
  ): ConduitResponse<UserRsp>
}
