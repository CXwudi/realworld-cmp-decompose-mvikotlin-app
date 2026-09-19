package mikufan.cx.conduit.frontend.logic.repo.kstore

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json


fun getAppDirectories(): AppStoreDirectories {
  val parentPath = Path("kstore")
  val files = Path(parentPath, "files")
  val caches = Path(parentPath, "caches")
  with(SystemFileSystem) {
    if (!exists(files)) createDirectories(files)
    if (!exists(caches)) createDirectories(caches)
  }

  return AppStoreDirectories(
    files = files,
    caches = caches
  )
}

fun setupPersistedConfigKStore(appStoreDirectories: AppStoreDirectories): KStore<PersistedConfig> {
  return storeOf(
    file = Path(appStoreDirectories.files, "persisted-config.json"),
    default = PersistedConfig(),
    json = Json {
      prettyPrint = true
    }
  )
}