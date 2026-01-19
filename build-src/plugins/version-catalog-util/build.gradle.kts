import com.github.gmazzo.buildconfig.BuildConfigClassSpec

plugins {
  `embedded-kotlin`
  alias(libs.plugins.buildConfig)
}

buildConfig {
  generateAtSync = false
  useKotlinOutput { internalVisibility = false }
  forClass(packageName = "my.util", className = "Versions") {
    buildConfigIntField("Java", libs.versions.java)
    buildConfigIntField("AndroidCompileSdk", libs.versions.android.compileSdk)
    buildConfigIntField("AndroidTargetSdk", libs.versions.android.targetSdk)
    buildConfigIntField("AndroidMinSdk", libs.versions.android.minSdk)
  }
  forClass(packageName = "my.util", className = "Libs") {
    // Serialization
    buildConfigStringField("SerializationJson", libs.dev.serializationJson)

    // Coroutines
    buildConfigStringField("CoroutinesBom", libs.dev.coroutinesBom)

    // Essenty
    buildConfigStringField("EssentyLifecycleCoroutines", libs.dev.frontend.essentyLifecycleCoroutines)

    // Decompose
    buildConfigStringField("Decompose", libs.dev.frontend.decompose)
    buildConfigStringField("DecomposeCompose", libs.dev.frontend.decomposeCompose)

    // Compose Multiplatform
    buildConfigStringField("ComposeRuntime", libs.dev.frontend.compose.runtime)
    buildConfigStringField("ComposeUi", libs.dev.frontend.compose.ui)
    buildConfigStringField("ComposeFoundation", libs.dev.frontend.compose.foundation)
    buildConfigStringField("ComposeMaterial3", libs.dev.frontend.compose.material3)
    buildConfigStringField("ComposeResources", libs.dev.frontend.compose.resources)
    buildConfigStringField("ComposeUiTooling", libs.dev.frontend.compose.uiTooling)
    buildConfigStringField("ComposeUiToolingPreview", libs.dev.frontend.compose.uiToolingPreview)
    buildConfigStringField("ComposeMaterial3AdaptiveNavigationSuite", libs.dev.frontend.compose.material3AdaptiveNavigationSuite)
    buildConfigStringField("ComposeDesktopCommon", libs.dev.frontend.compose.desktopCommon)

    // MVI Kotlin
    // likely this is only needed for decompose-logic module
//    buildConfigStringField("MviKotlin", libs.dev.frontend.mvikotlin)
//    buildConfigStringField("MviKotlinMain", libs.dev.frontend.mvikotlinMain)
//    buildConfigStringField("MviKotlinCoroutines", libs.dev.frontend.mvikotlinCoroutines)
//    buildConfigStringField("MviKotlinLogging", libs.dev.frontend.mvikotlinLogging)

    // Logging
    buildConfigStringField("KotlinLogging", libs.dev.kotlinLogging)
    buildConfigStringField("Slf4jAndroid", libs.dev.frontend.slf4jAndroid)
    buildConfigStringField("Logback", libs.dev.backend.logback)

    // Koin
    buildConfigStringField("KoinBom", libs.dev.koinBom)

    // Ktor
    buildConfigStringField("KtorBom", libs.dev.frontend.ktorBom)

    // Kotlin Wrapper
    buildConfigStringField("KotlinWrapper", libs.dev.frontend.kotlinWrapper)

    // AndroidX
    buildConfigStringField("AndroidXCoreKtx", libs.dev.frontend.androidx.coreKtx)
    buildConfigStringField("AndroidXLifecycleKtx", libs.dev.frontend.androidx.lifecycleKtx)
    buildConfigStringField("AndroidXAppCompat", libs.dev.frontend.androidx.appcompat)
    buildConfigStringField("AndroidXActivityCompose", libs.dev.frontend.androidx.activityCompose)
  }
}

fun BuildConfigClassSpec.buildConfigStringField(name: String, version: Provider<*>) {
  buildConfigField(String::class.java, name, version.map { "$it" })
}

fun BuildConfigClassSpec.buildConfigIntField(name: String, version: Provider<String>) {
  buildConfigField(Int::class.java, name, version.map { it.toInt() })
}