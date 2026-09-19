package mikufan.cx.conduit.frontend.logic.repo

import de.jensklingenberg.ktorfit.Ktorfit
import mikufan.cx.conduit.frontend.logic.repo.api.createArticleApi
import mikufan.cx.conduit.frontend.logic.repo.api.createAuthApi
import mikufan.cx.conduit.frontend.logic.repo.api.createDefaultHttpClient
import mikufan.cx.conduit.frontend.logic.repo.api.createKtorfit
import mikufan.cx.conduit.frontend.logic.repo.api.defaultJson
import mikufan.cx.conduit.frontend.logic.repo.api.util.ConduitResponseConverterFactory
import mikufan.cx.conduit.frontend.logic.repo.kstore.KStoreKey
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStoreImpl
import mikufan.cx.conduit.frontend.logic.repo.kstore.kstoreKmpModule
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module


val kstoreModule = module {
  includes(kstoreKmpModule)
  single<UserConfigKStore> { UserConfigKStoreImpl(get(named(KStoreKey.PERSISTED_CONFIG))) }
}

/**
 * Require [kstoreModule] due to the ktor client plugin that intercept the URL and token.
 */
val apiModule = module {
  // Originally, we were planning to use a StateFlow<HttpClient> that reflect the user config changes,
  // but we decided to just create a static all-in-one HttpClient,
  // and let all Ktorfit interfaces accept URL and token as parameters.
  // This also makes a better performance since we don't need to create a new HttpClient for each user config change.
  // See https://bbasoglu.medium.com/part-1-how-to-change-base-url-on-runtime-in-an-android-project-1d7607bbfa48

  singleOf(::defaultJson)
  singleOf(::createDefaultHttpClient)
  singleOf(::ConduitResponseConverterFactory)
  singleOf(::createKtorfit)
  single { get<Ktorfit>().createAuthApi() }
  single { get<Ktorfit>().createArticleApi() }
}

val repoModules = listOf(kstoreModule, apiModule)


