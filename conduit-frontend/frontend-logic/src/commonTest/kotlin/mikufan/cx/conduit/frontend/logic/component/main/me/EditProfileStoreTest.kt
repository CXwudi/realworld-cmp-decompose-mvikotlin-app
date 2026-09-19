package mikufan.cx.conduit.frontend.logic.component.main.me

import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.ExperimentalMviKotlinApi
import com.arkivanov.mvikotlin.extensions.coroutines.labelsChannel
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mikufan.cx.conduit.frontend.logic.service.main.EditProfileService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditProfileStoreTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var editProfileService: EditProfileService
  private lateinit var editProfileStore: Store<EditProfileIntent, EditProfileState, EditProfileLabel>

  private val initialState = EditProfileState(
    email = "test@email.com",
    username = "testuser",
    bio = "test bio",
    imageUrl = "test image url"
  )

  @BeforeTest
  fun setUp() {
    editProfileService = mock()
    editProfileStore = EditProfileStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      editProfileService,
      testDispatcher,
    ).createStore(initialState)
  }

  @AfterTest
  fun reset() {
    editProfileStore.dispose()
  }

  @Test
  fun testEmailChange() = runTest(testDispatcher) {
    val stateChannel = Channel<EditProfileState>()

    // Given
    val newEmail = "newemail@test.com"

    // When
    val disposable = editProfileStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    editProfileStore.accept(EditProfileIntent.EmailChanged(newEmail))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(newEmail, newState.email)
    assertEquals(initialState.username, newState.username) // other fields should remain unchanged
    assertEquals(initialState.bio, newState.bio)
    assertEquals(initialState.imageUrl, newState.imageUrl)

    disposable.dispose()
  }

  @Test
  fun testUsernameChange() = runTest(testDispatcher) {
    val stateChannel = Channel<EditProfileState>()

    // Given
    val newUsername = "newusername"

    // When
    val disposable = editProfileStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    editProfileStore.accept(EditProfileIntent.UsernameChanged(newUsername))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(newUsername, newState.username)
    assertEquals(initialState.email, newState.email) // other fields should remain unchanged
    assertEquals(initialState.bio, newState.bio)
    assertEquals(initialState.imageUrl, newState.imageUrl)

    disposable.dispose()
  }

  @Test
  fun testBioChange() = runTest(testDispatcher) {
    val stateChannel = Channel<EditProfileState>()

    // Given
    val newBio = "new test bio"

    // When
    val disposable = editProfileStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    editProfileStore.accept(EditProfileIntent.BioChanged(newBio))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(newBio, newState.bio)
    assertEquals(initialState.email, newState.email) // other fields should remain unchanged
    assertEquals(initialState.username, newState.username)
    assertEquals(initialState.imageUrl, newState.imageUrl)

    disposable.dispose()
  }

  @Test
  fun testImageUrlChange() = runTest(testDispatcher) {
    val stateChannel = Channel<EditProfileState>()

    // Given
    val newImageUrl = "https://new-image-url.com"

    // When
    val disposable = editProfileStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    editProfileStore.accept(EditProfileIntent.ImageUrlChanged(newImageUrl))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(newImageUrl, newState.imageUrl)
    assertEquals(initialState.email, newState.email) // other fields should remain unchanged
    assertEquals(initialState.username, newState.username)
    assertEquals(initialState.bio, newState.bio)

    disposable.dispose()
  }

  @Test
  fun testPasswordChange() = runTest(testDispatcher) {
    val stateChannel = Channel<EditProfileState>()

    // Given
    val newPassword = "newpassword123"

    // When
    val disposable = editProfileStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    editProfileStore.accept(EditProfileIntent.PasswordChanged(newPassword))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(newPassword, newState.password)
    assertEquals(initialState.email, newState.email) // other fields should remain unchanged
    assertEquals(initialState.username, newState.username)
    assertEquals(initialState.bio, newState.bio)
    assertEquals(initialState.imageUrl, newState.imageUrl)

    disposable.dispose()
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testSaveSuccessful() = runTest(testDispatcher) {
    val stateChannel = Channel<EditProfileState>()
    val testScope = TestScope(testDispatcher)
    val labelChannel = editProfileStore.labelsChannel(testScope)

    // When
    val disposable = editProfileStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))

    everySuspend { editProfileService.updateAndSave(any()) } returns Unit
     editProfileStore.accept(EditProfileIntent.Save)

    // Then
    stateChannel.receive() // ignore the initial state
    val newLabel = labelChannel.receive() // wait until the label is emitted
    verifySuspend(exactly(1)) { editProfileService.updateAndSave(any()) }
    assertTrue { newLabel is EditProfileLabel.SaveSuccessLabel }

    disposable.dispose()
  }

  @Test
  fun testSaveFailed() = runTest(testDispatcher) {
    val stateChannel = Channel<EditProfileState>()

    // When
    val disposable = editProfileStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    val errorMessage = "Failed to update profile"
    everySuspend { editProfileService.updateAndSave(any()) } throws RuntimeException(errorMessage)
    editProfileStore.accept(EditProfileIntent.Save)

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(errorMessage, newState.errorMsg)
    verifySuspend(exactly(1)) { editProfileService.updateAndSave(any()) }

    disposable.dispose()
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testBackWithoutSave() = runTest(testDispatcher) {
    val testScope = TestScope(testDispatcher)
    val labelChannel = editProfileStore.labelsChannel(testScope)

    // When
    editProfileStore.accept(EditProfileIntent.BackWithoutSave)

    // Then
    val label = labelChannel.receive()
    assertTrue(label is EditProfileLabel.BackWithoutSave)
  }
}
