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
import mikufan.cx.conduit.frontend.logic.service.main.AddArticleService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddArticleStoreTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var addArticleService: AddArticleService
  private lateinit var addArticleStore: Store<AddArticleIntent, AddArticleState, AddArticleLabel>

  @BeforeTest
  fun setUp() {
    addArticleService = mock()
    addArticleStore = AddArticleStoreFactory(
      LoggingStoreFactory(DefaultStoreFactory()),
      addArticleService,
      testDispatcher,
    ).createStore()
  }

  @AfterTest
  fun reset() {
    addArticleStore.dispose()
  }

  @Test
  fun testTitleChange() = runTest(testDispatcher) {
    val stateChannel = Channel<AddArticleState>()

    // Given
    val newTitle = "New Test Article"

    // When
    val disposable =
      addArticleStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    addArticleStore.accept(AddArticleIntent.TitleChanged(newTitle))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(newTitle, newState.title)
    assertEquals("", newState.description) // other fields should remain unchanged
    assertEquals("", newState.body)
    assertTrue(newState.tagList.isEmpty())

    disposable.dispose()
  }

  @Test
  fun testDescriptionChange() = runTest(testDispatcher) {
    val stateChannel = Channel<AddArticleState>()

    // Given
    val newDescription = "New test description"

    // When
    val disposable =
      addArticleStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    addArticleStore.accept(AddArticleIntent.DescriptionChanged(newDescription))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(newDescription, newState.description)
    assertEquals("", newState.title) // other fields should remain unchanged
    assertEquals("", newState.body)
    assertTrue(newState.tagList.isEmpty())

    disposable.dispose()
  }

  @Test
  fun testBodyChange() = runTest(testDispatcher) {
    val stateChannel = Channel<AddArticleState>()

    // Given
    val newBody = """
      # Some Title
      
      Some body content
      
      `some code`
      
      * some list
      * another list
    """.trimIndent()

    // When
    val disposable =
      addArticleStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    addArticleStore.accept(AddArticleIntent.BodyChanged(newBody))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(newBody, newState.body)
    assertEquals("", newState.title) // other fields should remain unchanged
    assertEquals("", newState.description)
    assertTrue(newState.tagList.isEmpty())

    disposable.dispose()
  }

  @Test
  fun testTagListChange() = runTest(testDispatcher) {
    val stateChannel = Channel<AddArticleState>()

    // Given
    val tagString = "kotlin,mvikotlin,testing"
    val expectedTags = listOf("kotlin", "mvikotlin", "testing")

    // When
    val disposable =
      addArticleStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    addArticleStore.accept(AddArticleIntent.TagListChanged(tagString))

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(expectedTags, newState.tagList)
    assertEquals("", newState.title) // other fields should remain unchanged
    assertEquals("", newState.description)
    assertEquals("", newState.body)

    disposable.dispose()
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testPublishSuccessful() = runTest(testDispatcher) {
    val stateChannel = Channel<AddArticleState>()
    val testScope = TestScope(testDispatcher)
    val labelChannel = addArticleStore.labelsChannel(testScope)

    // When
    val disposable =
      addArticleStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    everySuspend { addArticleService.createArticle(any(), any(), any(), any()) } returns Unit
    addArticleStore.accept(AddArticleIntent.Publish)

    // Then
    stateChannel.receive() // ignore the initial state
    val newLabel = labelChannel.receive() // wait until the label is emitted
    verifySuspend(exactly(1)) { addArticleService.createArticle(any(), any(), any(), any()) }
    assertTrue { newLabel is AddArticleLabel.PublishSuccess }

    disposable.dispose()
  }

  @Test
  fun testPublishFailed() = runTest(testDispatcher) {
    val stateChannel = Channel<AddArticleState>()

    // When
    val disposable =
      addArticleStore.states(observer(onNext = { this.launch { stateChannel.send(it) } }))
    val errorMessage = "Failed to publish article"
    everySuspend {
      addArticleService.createArticle(
        any(),
        any(),
        any(),
        any()
      )
    } throws RuntimeException(errorMessage)
    addArticleStore.accept(AddArticleIntent.Publish)

    // Then
    stateChannel.receive() // ignore the initial state
    val newState = stateChannel.receive()
    assertEquals(errorMessage, newState.errorMsg)
    verifySuspend(exactly(1)) { addArticleService.createArticle(any(), any(), any(), any()) }

    disposable.dispose()
  }

  @OptIn(ExperimentalMviKotlinApi::class)
  @Test
  fun testBackWithoutPublish() = runTest(testDispatcher) {
    val testScope = TestScope(testDispatcher)
    val labelChannel = addArticleStore.labelsChannel(testScope)

    // When
    addArticleStore.accept(AddArticleIntent.BackWithoutPublish)

    // Then
    val label = labelChannel.receive()
    assertTrue(label is AddArticleLabel.BackWithoutPublish)
  }
}