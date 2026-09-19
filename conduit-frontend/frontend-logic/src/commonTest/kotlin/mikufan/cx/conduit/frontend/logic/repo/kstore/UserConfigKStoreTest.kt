package mikufan.cx.conduit.frontend.logic.repo.kstore

import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.github.xxfast.kstore.Codec
import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserConfigKStoreTest {

  lateinit var codec: Codec<PersistedConfig>
  lateinit var userConfigKStore: UserConfigKStore

  @BeforeTest
  fun setUp() {
    codec = mock<Codec<PersistedConfig>> {
      everySuspend { decode() } returns PersistedConfig()
    }
    val kstore = KStore(
      PersistedConfig(),
      codec = codec
    )
    userConfigKStore = UserConfigKStoreImpl(kstore)
  }

  @Test
  fun testSetUrlNormalFlow() = runTest {
    userConfigKStore.setUrl("some url")
    verifySuspend(exactly(1)) {
      codec.decode()
      codec.encode(any())
    }
  }

  @Test
  fun testSetUrlErrorFlow() = runTest {
    val exp = assertFailsWith<IllegalArgumentException> {
      userConfigKStore.setUrl("")
    }
    assertEquals("url cannot be empty", exp.message)
  }
}