package mikufan.cx.conduit.frontend.logic.component.main.feed

import kotlin.time.Instant
import kotlinx.serialization.Serializable


sealed interface ArticlesListIntent {
  data object LoadMore : ArticlesListIntent
  data class ClickOnArticle(val articleBasicInfo: ArticleBasicInfo) : ArticlesListIntent
}

data class ArticlesListState(
  val collectedThumbInfos: List<ArticleInfo> = emptyList(),
  val loadMoreState: LoadMoreState = LoadMoreState.Loaded
)

enum class LoadMoreState { Loading, Loaded }

@Serializable
data class ArticleInfo(
  val authorThumbnail: String?,
  val authorUsername: String,
  val title: String,
  val description: String,
  val tags: List<String>,
  val createdAt: Instant,
  val slug: String,
) {
  fun toBasicInfo(): ArticleBasicInfo = ArticleBasicInfo(
    authorThumbnail = authorThumbnail,
    authorUsername = authorUsername,
    title = title,
    slug = slug,
  )
}

sealed interface ArticlesListLabel {
  data class Failure(
    val exception: Exception?,
    val message: String,
  ) : ArticlesListLabel
  data class OpenArticle(val basicInfo: ArticleBasicInfo) : ArticlesListLabel
}
