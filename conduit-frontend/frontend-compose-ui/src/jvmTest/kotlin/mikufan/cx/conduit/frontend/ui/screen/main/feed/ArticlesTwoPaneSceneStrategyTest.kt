package mikufan.cx.conduit.frontend.ui.screen.main.feed

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SceneStrategyScope
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleBasicInfo
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesNavRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticlesTwoPaneSceneStrategyTest {

  private val scope = SceneStrategyScope<ArticlesNavRoute>()

  private val listRoute: ArticlesNavRoute = ArticlesNavRoute.List("list")
  private val detailRoute: ArticlesNavRoute = ArticlesNavRoute.Detail(
    basicInfo = ArticleBasicInfo(
      authorThumbnail = "thumb.png",
      authorUsername = "alice",
      title = "Scene Strategy Test",
      slug = "scene-strategy-test",
    ),
    entryId = "detail_1",
  )

  private val listEntry: NavEntry<ArticlesNavRoute> = NavEntry(
    key = listRoute,
    contentKey = "List_list",
  ) {}

  private val detailEntry: NavEntry<ArticlesNavRoute> = NavEntry(
    key = detailRoute,
    contentKey = "Detail_detail_1",
  ) {}

  @Test
  fun testCompactModeReturnsNullForSinglePaneFallback() {
    val strategy = ArticlesTwoPaneSceneStrategy<ArticlesNavRoute>(isDualPane = false)
    val entries = listOf(listEntry, detailEntry)

    val scene = with(strategy) { scope.calculateScene(entries) }
    assertNull(scene, "Compact mode must return null so NavDisplay falls back to SinglePaneSceneStrategy")
  }

  @Test
  fun testDualModeWithSingleListEntryReturnsNull() {
    val strategy = ArticlesTwoPaneSceneStrategy<ArticlesNavRoute>(isDualPane = true)
    val entries = listOf(listEntry)

    val scene = with(strategy) { scope.calculateScene(entries) }
    assertNull(scene, "Dual mode with only list entry must return null to display list in full width")
  }

  @Test
  fun testDualModeWithListAndDetailReturnsTwoPaneScene() {
    val strategy = ArticlesTwoPaneSceneStrategy<ArticlesNavRoute>(isDualPane = true)
    val entries = listOf(listEntry, detailEntry)

    val scene = with(strategy) { scope.calculateScene(entries) }
    assertNotNull(scene)
    assertTrue(scene is ArticlesTwoPaneScene)

    assertEquals(2, scene.entries.size)
    assertEquals<NavEntry<ArticlesNavRoute>>(listEntry, scene.entries[0])
    assertEquals<NavEntry<ArticlesNavRoute>>(detailEntry, scene.entries[1])

    // On back, previous entries must be list-only
    assertEquals(listOf(listEntry), scene.previousEntries)
    assertEquals("TwoPane_List_list_Detail_detail_1", scene.key)
  }

  @Test
  fun testStrategyEqualityAndHashCode() {
    val strategy1 = ArticlesTwoPaneSceneStrategy<ArticlesNavRoute>(isDualPane = true)
    val strategy2 = ArticlesTwoPaneSceneStrategy<ArticlesNavRoute>(isDualPane = true)
    val strategyCompact = ArticlesTwoPaneSceneStrategy<ArticlesNavRoute>(isDualPane = false)

    assertEquals(strategy1, strategy2)
    assertEquals(strategy1.hashCode(), strategy2.hashCode())
    assertNotEquals(strategy1, strategyCompact)
  }
}
