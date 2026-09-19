package mikufan.cx.conduit.frontend.logic.repo.kstore

import org.koin.core.module.Module

/**
 * Due to the android setup requires Application instance, we need to use this expect module to inject it.
 */
expect val kstoreKmpModule: Module

