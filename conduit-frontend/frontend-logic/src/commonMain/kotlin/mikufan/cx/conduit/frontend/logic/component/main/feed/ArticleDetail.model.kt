package mikufan.cx.conduit.frontend.logic.component.main.feed

import kotlin.time.Instant
import kotlinx.serialization.Serializable


sealed interface ArticleDetailIntent {
  data object BackToList : ArticleDetailIntent
}

data class ArticleDetailState(
  val basicInfo: ArticleBasicInfo,
  val detailInfo: ArticleDetailInfo? = null
)

@Serializable
data class ArticleBasicInfo(
  val authorThumbnail: String?,
  val authorUsername: String,
  val title: String,
  val slug: String
)

data class ArticleDetailInfo(
  val description: String,
  val bodyMarkdown: String,
  val tags: List<String>,
  val createdAt: Instant
)

sealed interface ArticleDetailLabel {
  data object BackToList : ArticleDetailLabel
  data class Failure(
    val exception: Exception? = null,
    val message: String,
  ) : ArticleDetailLabel
}
