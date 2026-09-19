package mikufan.cx.conduit.frontend.logic.service

import mikufan.cx.conduit.frontend.logic.repo.repoModules
import mikufan.cx.conduit.frontend.logic.service.landing.DefaultLandingService
import mikufan.cx.conduit.frontend.logic.service.landing.LandingService
import mikufan.cx.conduit.frontend.logic.service.main.AddArticleService
import mikufan.cx.conduit.frontend.logic.service.main.ArticleDetailService
import mikufan.cx.conduit.frontend.logic.service.main.ArticlesListService
import mikufan.cx.conduit.frontend.logic.service.main.AuthService
import mikufan.cx.conduit.frontend.logic.service.main.DefaultAddArticleService
import mikufan.cx.conduit.frontend.logic.service.main.DefaultArticleDetailService
import mikufan.cx.conduit.frontend.logic.service.main.DefaultArticlesListService
import mikufan.cx.conduit.frontend.logic.service.main.DefaultAuthService
import mikufan.cx.conduit.frontend.logic.service.main.DefaultEditProfileService
import mikufan.cx.conduit.frontend.logic.service.main.DefaultMePageService
import mikufan.cx.conduit.frontend.logic.service.main.EditProfileService
import mikufan.cx.conduit.frontend.logic.service.main.MePageService
import org.koin.dsl.module


/**
 * Require [repoModules]
 */
val serviceModule = module {
  single<LandingService> { DefaultLandingService(get(), get()) }
  single<AuthService> { DefaultAuthService(get(), get()) }
  single<MePageService> { DefaultMePageService(get(), get()) }
  single<EditProfileService> { DefaultEditProfileService(get(), get()) }
  single<AddArticleService> { DefaultAddArticleService(get()) }
  single<ArticlesListService> { DefaultArticlesListService(get()) }
  single<ArticleDetailService> { DefaultArticleDetailService(get()) }
}
