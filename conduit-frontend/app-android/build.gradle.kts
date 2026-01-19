plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinCompose)
  alias(libs.plugins.compose)
}

dependencies {
  implementation(project(":frontend-decompose-logic"))
  implementation(project(":frontend-compose-ui"))

  // Explicitly declare dependencies we directly import in MainActivity/MainApplication
  implementation(libs.dev.frontend.decompose)
  implementation(libs.dev.kotlinLogging)
  implementation(platform(libs.dev.koinBom))
  implementation("io.insert-koin:koin-core")
  implementation("io.insert-koin:koin-android")
  implementation("io.insert-koin:koin-androidx-startup")

  // Android-specific dependencies
  implementation(libs.dev.frontend.compose.uiToolingPreview)
  implementation(libs.dev.frontend.compose.uiTooling)
  implementation(libs.dev.frontend.androidx.appcompat)
  implementation(libs.dev.frontend.androidx.coreKtx)
  implementation(libs.dev.frontend.androidx.activityCompose)
  implementation(libs.dev.frontend.slf4jAndroid)
}

android {
  namespace = "mikufan.cx.conduit.frontend.app.android"
  compileSdk = libs.versions.android.compileSdk.get().toInt()
  defaultConfig {
    applicationId = "mikufan.cx.conduit.frontend.app.android"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
}
