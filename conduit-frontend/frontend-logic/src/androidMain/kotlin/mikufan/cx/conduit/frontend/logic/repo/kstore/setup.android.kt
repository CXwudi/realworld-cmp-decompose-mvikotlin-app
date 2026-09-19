package mikufan.cx.conduit.frontend.logic.repo.kstore

import android.app.Application
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path

fun getAppDirectories(application: Application): AppStoreDirectories {

  return AppStoreDirectories(
    files = Path(application.filesDir.toString()),
    caches = Path(application.cacheDir.toString()),
  )
}

fun setupPersistedConfigKStore(appStoreDirectories: AppStoreDirectories): KStore<PersistedConfig> {
  return storeOf(
    file = Path(appStoreDirectories.files, "persisted-config.json"),
    default = PersistedConfig(),
  )
}