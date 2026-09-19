package mikufan.cx.conduit.frontend.logic.repo.kstore

import android.app.Application
import android.content.Context
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val kstoreKmpModule: Module = module {
  single { getAppDirectories(get<Context>() as Application) }
  single(named(KStoreKey.PERSISTED_CONFIG)) { setupPersistedConfigKStore(get()) }
}