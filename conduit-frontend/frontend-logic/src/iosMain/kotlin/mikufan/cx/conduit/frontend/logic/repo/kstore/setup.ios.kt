package mikufan.cx.conduit.frontend.logic.repo.kstore

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path

//import platform.Foundation.NSDocumentDirectory
//import platform.Foundation.NSFileManager
//import platform.Foundation.NSStringFromClass
//import platform.Foundation.NSURL
//import platform.Foundation.NSUserDomainMask

fun getAppDirectories(): AppStoreDirectories {
//  val fileManager:NSFileManager = NSFileManager.defaultManager
//  val documentsUrl: NSURL = fileManager.URLForDirectory(
//    directory = NSDocumentDirectory,
//    appropriateForURL = null,
//    create = false,
//    inDomain = NSUserDomainMask,
//    error = null
//  )!!
//
//  val cachesUrl:NSURL = fileManager.URLForDirectory(
//    directory = NSCachesDirectory,
//    appropriateForURL = null,
//    create = false,
//    inDomain = NSUserDomainMask,
//    error = null
//  )!!
//
//  return AppStoreDirectories(
//    files =  Path(documentsUrl.path),
//    caches = Path(cachesUrl.path),
//  )
  TODO("Uncomment above when in a mac machine")
}

fun setupPersistedConfigKStore(appStoreDirectories: AppStoreDirectories): KStore<PersistedConfig> {
  return storeOf(
    file = Path(appStoreDirectories.files, "persisted-config.json"),
    default = PersistedConfig(),
  )
}