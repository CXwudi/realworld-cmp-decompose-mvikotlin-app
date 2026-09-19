package mikufan.cx.conduit.frontend.logic.repo.kstore

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf

object StoreKeys {
  const val PERSISTED_CONFIG = "persisted_config"
}

fun setupPersistedConfigKStore(): KStore<PersistedConfig> = storeOf(StoreKeys.PERSISTED_CONFIG, default = PersistedConfig())