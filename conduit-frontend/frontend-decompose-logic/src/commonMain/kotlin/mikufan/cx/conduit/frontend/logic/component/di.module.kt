package mikufan.cx.conduit.frontend.logic.component

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import mikufan.cx.conduit.frontend.logic.component.landing.LandingPageComponentFactory
import mikufan.cx.conduit.frontend.logic.component.landing.LandingPageStoreFactory
import mikufan.cx.conduit.frontend.logic.component.landing.LandingViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.legacy.LegacyChildAdapterViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.legacy.LegacyMainAdapterViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.DefaultMainNavComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.MainNavComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.MainNavStoreFactory
import mikufan.cx.conduit.frontend.logic.component.main.MainNavViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageStoreFactory
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleDetailComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleDetailStoreFactory
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListStoreFactory
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesPanelNavComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.AddArticleComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.AddArticleStoreFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.AddArticleViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.EditProfileComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.EditProfileStoreFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.EditProfileViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavStoreFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.MeNavViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageComponentFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageViewModelFactory
import mikufan.cx.conduit.frontend.logic.component.main.me.MeStoreFactory
import mikufan.cx.conduit.frontend.logic.component.root.RootViewModelFactory
import mikufan.cx.conduit.frontend.logic.service.serviceModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Require [serviceModule]
 */
val storeModule = module {
//  single<StoreFactory> { LoggingStoreFactory(DefaultStoreFactory()) } // un-comment to debug, very verbose!
  single<StoreFactory> { DefaultStoreFactory() }
  // can't use singleOf on constructor with default parameters
  single { LandingPageStoreFactory(get(), get()) }
  single { MainNavStoreFactory(get(), get()) }
  single { AuthPageStoreFactory(get(), get()) }
  single { MeNavStoreFactory(get()) }
  single { MeStoreFactory(get(), get()) }
  single { EditProfileStoreFactory(get(), get()) }
  single { AddArticleStoreFactory(get(), get()) }
  single { ArticlesListStoreFactory(get(), get()) }
  single { ArticleDetailStoreFactory(get(), get()) }
}

/**
 * Require [storeModule]
 */
val componentFactoryModule = module {
  singleOf(::RootNavComponentFactory)
  singleOf(::RootViewModelFactory)
  singleOf(::LandingPageComponentFactory)
  singleOf(::LandingViewModelFactory)
  singleOf(::MainNavViewModelFactory)
  singleOf(::AuthViewModelFactory)
  singleOf(::MeNavViewModelFactory)
  singleOf(::MePageViewModelFactory)
  singleOf(::EditProfileViewModelFactory)
  singleOf(::AddArticleViewModelFactory)
  singleOf(::LegacyChildAdapterViewModelFactory)
  singleOf(::LegacyMainAdapterViewModelFactory)
  singleOf(::DefaultMainNavComponentFactory) bind MainNavComponentFactory::class
  singleOf(::AuthPageComponentFactory)
  singleOf(::MeNavComponentFactory)
  singleOf(::MePageComponentFactory)
  singleOf(::EditProfileComponentFactory)
  singleOf(::AddArticleComponentFactory)
  singleOf(::ArticlesPanelNavComponentFactory)
  singleOf(::ArticlesListComponentFactory)
  singleOf(::ArticleDetailComponentFactory)
}

val decomposeViewModelModules = listOf(storeModule, componentFactoryModule)
