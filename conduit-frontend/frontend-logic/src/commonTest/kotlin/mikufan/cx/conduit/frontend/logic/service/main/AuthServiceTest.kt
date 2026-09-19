package mikufan.cx.conduit.frontend.logic.service.main

import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mikufan.cx.conduit.common.UserDto
import mikufan.cx.conduit.common.UserDtoUtils
import mikufan.cx.conduit.common.UserRsp
import mikufan.cx.conduit.frontend.logic.repo.api.AuthApi
import mikufan.cx.conduit.frontend.logic.repo.api.util.ConduitResponse
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserInfo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthServiceTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var authApi: AuthApi
  private lateinit var userConfigKStore: UserConfigKStore
  private lateinit var authService: AuthService

  @BeforeTest
  fun setUp() {
    authApi = mock()
    userConfigKStore = mock()
    authService = DefaultAuthService(authApi, userConfigKStore)
  }

  @Test
  fun testLogin() = runTest(testDispatcher) {
    // given
    val email = "test@test.com"
    val password = "password123"
    val token = "jwt.token.here"
    val loginReq = UserDtoUtils.createLoginReq(email, password)
    val userRsp = UserRsp(UserDto(
      email = email,
      username = "testuser",
      bio = null,
      image = null,
      token = token
    ))
    val response = ConduitResponse.success(userRsp)

    // when
    everySuspend { authApi.login(loginReq) } returns response
    authService.login(email, password)

    // then
    verifySuspend(exactly(1)) { authApi.login(loginReq) }
    verifySuspend(exactly(1)) { 
      userConfigKStore.setUserInfo(UserInfo(
        email = email,
        username = "testuser",
        bio = null,
        image = null,
        token = token
      ))
    }
  }

  @Test
  fun testLoginWithNullToken() = runTest(testDispatcher) {
    // given
    val email = "test@test.com"
    val password = "password123"
    val loginReq = UserDtoUtils.createLoginReq(email, password)
    val userRsp = UserRsp(UserDto(
      email = email,
      username = "testuser",
      bio = null,
      image = null,
      token = null
    ))
    val response = ConduitResponse.success(userRsp)

    // when
    everySuspend { authApi.login(loginReq) } returns response

    // then
    val exp = assertFailsWith<IllegalStateException> {
      authService.login(email, password)
    }

    assertEquals("Token is null or blank", exp.message)
  }

  @Test
  fun testRegister() = runTest(testDispatcher) {
    // given
    val email = "test@test.com"
    val username = "testuser"
    val password = "password123"
    val token = "jwt.token.here"
    val registerReq = UserDtoUtils.createRegisterReq(email, username, password)
    val userRsp = UserRsp(UserDto(
      email = email,
      username = username,
      bio = null,
      image = null,
      token = token
    ))
    val response = ConduitResponse.success(userRsp)

    // when
    everySuspend { authApi.register(registerReq) } returns response
    authService.register(email, username, password)

    // then
    verifySuspend(exactly(1)) { authApi.register(registerReq) }
    verifySuspend(exactly(1)) { 
      userConfigKStore.setUserInfo(UserInfo(
        email = email,
        username = username,
        bio = null,
        image = null,
        token = token
      ))
    }
  }

  @Test
  fun testRegisterWithNullToken() = runTest(testDispatcher) {
    // given
    val email = "test@test.com"
    val username = "testuser"
    val password = "password123"
    val registerReq = UserDtoUtils.createRegisterReq(email, username, password)
    val userRsp = UserRsp(UserDto(
      email = email,
      username = username,
      bio = null,
      image = null,
      token = null
    ))
    val response = ConduitResponse.success(userRsp)

    // when
    everySuspend { authApi.register(registerReq) } returns response

    // then
    val exp = assertFailsWith<IllegalStateException> {
      authService.register(email, username, password)
    }

    assertEquals("Token is null or blank", exp.message)
  }

  @Test
  fun testReset() = runTest(testDispatcher) {
    // when
    authService.reset()

    // then
    verifySuspend(exactly(1)) { userConfigKStore.reset() }
  }
}
