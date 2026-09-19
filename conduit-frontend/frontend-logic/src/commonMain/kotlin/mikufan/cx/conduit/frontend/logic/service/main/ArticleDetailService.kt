package mikufan.cx.conduit.frontend.logic.service.main

import mikufan.cx.conduit.common.ArticleDto
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleDetailInfo
import mikufan.cx.conduit.frontend.logic.repo.api.ArticleApi
import mikufan.cx.conduit.frontend.logic.repo.api.util.getOrThrow
import kotlin.time.ExperimentalTime

interface ArticleDetailService {
  /**
   * Get the full article details by slug.
   */
  suspend fun getArticle(slug: String): ArticleDetailInfo
}

class DefaultArticleDetailService(
  private val articleApi: ArticleApi,
) : ArticleDetailService {

  override suspend fun getArticle(slug: String): ArticleDetailInfo {
    val articleRsp = articleApi.getArticle(slug)
    val articleDto = articleRsp.getOrThrow()
    return articleDto.article.toArticleDetailInfo()
  }
}

private fun ArticleDto.toArticleDetailInfo() = ArticleDetailInfo(
  description = description,
  bodyMarkdown = body ?: "", // body should not be null for individual article requests
  tags = tagList,
  createdAt = createdAt,
)
