package mikufan.cx.conduit.frontend.logic.component.main.me

import kotlinx.serialization.Serializable

/**
 * Plain serializable routes for the Me navigation stack.
 *
 * Each route has a stable [entryId] distinguishing instances, ensuring
 * that reopening an editor/add-article after popping produces a fresh identity.
 */
@Serializable
sealed interface MeNavRoute {
  val entryId: String

  @Serializable
  data class Profile(
    override val entryId: String = "profile",
  ) : MeNavRoute

  @Serializable
  data class EditProfile(
    val loadedMe: LoadedMe,
    override val entryId: String,
  ) : MeNavRoute

  @Serializable
  data class AddArticle(
    override val entryId: String,
  ) : MeNavRoute
}

/**
 * State of the Me navigation stack.
 */
data class MeNavState(
  val stack: List<MeNavRoute> = listOf(MeNavRoute.Profile()),
  val nextEntryId: Long = 1L,
) {
  val currentRoute: MeNavRoute
    get() = stack.last()
}

/**
 * Intents accepted by [MeNavStoreFactory]'s store.
 */
sealed interface MeNavIntent {
  /**
   * Pushes EditProfile route with [loadedMe]. Ignored if already on editor or add article (repeated click protection).
   */
  data class NavigateToEditProfile(val loadedMe: LoadedMe) : MeNavIntent

  /**
   * Pushes AddArticle route. Ignored if already on editor or add article (repeated click protection).
   */
  data object NavigateToAddArticle : MeNavIntent

  /**
   * Pops the route if [entryId] matches the top of the stack.
   * Stale or repeated completions with mismatched [entryId] are rejected.
   */
  data class PopRoute(val entryId: String) : MeNavIntent

  /**
   * Pops the top route if stack has more than 1 entry (used by back handler).
   */
  data object Pop : MeNavIntent
}

/**
 * Compact serializable snapshot persisted into SavedStateHandle for process-death recreation.
 */
@Serializable
data class MeNavSavedSnapshot(
  val stack: List<MeNavRoute>,
  val nextEntryId: Long,
)

/**
 * Stable navigation callback interface bound to [MeNavViewModel].
 * Does not capture any Composable, Activity, or mutable UI state.
 */
interface MeNavNavigator {
  fun onEditProfile(loadedMe: LoadedMe)
  fun onAddArticle()
  fun onPopRoute(entryId: String)
}
