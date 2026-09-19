package mikufan.cx.conduit.frontend.logic.component.main.feed

import kotlinx.serialization.Serializable

/**
 * Route representations for Feed / Favourite list-detail navigation stack.
 *
 * Backstack constraints:
 * - Minimum stack depth is 1 ([List]).
 * - Maximum stack depth is 2 ([List] + optional [Detail]).
 * - Selecting another article replaces the existing [Detail] entry, preserving [List].
 */
@Serializable
sealed interface ArticlesNavRoute {
  val entryId: String

  @Serializable
  data class List(
    override val entryId: String = "list",
  ) : ArticlesNavRoute

  @Serializable
  data class Detail(
    val basicInfo: ArticleBasicInfo,
    override val entryId: String,
  ) : ArticlesNavRoute
}

/**
 * Navigation state for the Feed / Favourite list-detail scene.
 */
data class ArticlesNavState(
  val stack: kotlin.collections.List<ArticlesNavRoute> = listOf(ArticlesNavRoute.List()),
  val nextEntryId: Long = 1L,
) {
  val currentRoute: ArticlesNavRoute
    get() = stack.last()

  val isDetailOpen: Boolean
    get() = stack.size > 1 && currentRoute is ArticlesNavRoute.Detail
}

/**
 * Intents accepted by [ArticlesNavStoreFactory]'s store.
 */
sealed interface ArticlesNavIntent {
  /**
   * Request to view article details. Replaces any existing detail entry.
   * If the currently viewed article has the same slug, the request is ignored.
   */
  data class OpenArticle(val basicInfo: ArticleBasicInfo) : ArticlesNavIntent

  /**
   * Request to close a specific detail entry by [entryId].
   * Protects against stale close events from an article that was already replaced.
   */
  data class CloseDetail(val entryId: String) : ArticlesNavIntent

  /**
   * General pop intent (e.g. from system back handler).
   * Closes the detail panel if open; never pops the root list.
   */
  data object Pop : ArticlesNavIntent
}

/**
 * Compact serializable snapshot for `SavedStateHandle` persistence.
 *
 * Preserves the route stack (including [ArticleBasicInfo]) and auto-incrementing ID.
 * Business caches, articles lists, and markdown content are excluded and reloaded.
 */
@Serializable
data class ArticlesNavSavedSnapshot(
  val stack: kotlin.collections.List<ArticlesNavRoute>,
  val nextEntryId: Long,
)

/**
 * Navigation coordinator interface implemented by [ArticlesNavViewModel].
 *
 * Provides a stable navigation sink for child leaf ViewModels ([ArticlesListViewModel],
 * [ArticleDetailViewModel]) without leaking UI, Activity, or Composable scopes.
 */
interface ArticlesNavNavigator {
  fun onOpenArticle(basicInfo: ArticleBasicInfo)
  fun onCloseDetail(entryId: String)
}
