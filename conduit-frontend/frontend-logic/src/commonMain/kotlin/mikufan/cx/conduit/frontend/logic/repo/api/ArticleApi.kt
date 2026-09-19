package mikufan.cx.conduit.frontend.logic.repo.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import mikufan.cx.conduit.common.ArticleRsp
import mikufan.cx.conduit.common.ArticlesRsp
import mikufan.cx.conduit.common.CreateArticleReq
import mikufan.cx.conduit.frontend.logic.repo.api.util.ConduitResponse

interface ArticleApi {
  @GET("articles")
  @Headers("Content-Type: application/json")
  suspend fun getArticles(
    @Query tag: String? = null,
    @Query author: String? = null,
    @Query favorited: String? = null,
    @Query limit: Int? = null,
    @Query offset: Int? = null
  ): ConduitResponse<ArticlesRsp>

  @POST("articles")
  @Headers("Content-Type: application/json")
  suspend fun createArticle(
    @Body body: CreateArticleReq
  ) : ConduitResponse<ArticleRsp>

  @GET("articles/{slug}")
  @Headers("Content-Type: application/json")
  suspend fun getArticle(
    @Path("slug") slug: String
  ) : ConduitResponse<ArticleRsp>
}