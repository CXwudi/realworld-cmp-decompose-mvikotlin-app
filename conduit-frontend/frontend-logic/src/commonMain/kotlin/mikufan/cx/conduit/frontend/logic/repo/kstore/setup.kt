package mikufan.cx.conduit.frontend.logic.repo.kstore

import kotlinx.io.files.Path

enum class KStoreKey {
  PERSISTED_CONFIG,
}

data class AppStoreDirectories(
  val files: Path,
  val caches: Path,
)