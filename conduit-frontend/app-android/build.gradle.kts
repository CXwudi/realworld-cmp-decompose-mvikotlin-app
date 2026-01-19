plugins {
  id("my.cmp-app")
  alias(libs.plugins.androidApplication)
}

kotlin {
  androidTarget()

  sourceSets {
    androidMain.dependencies {
      implementation(project(":frontend-decompose-logic"))
      implementation(project(":frontend-compose-ui"))

      implementation(libs.dev.frontend.compose.uiToolingPreview)
      implementation(libs.dev.frontend.compose.uiTooling)

      implementation(libs.dev.frontend.androidx.appcompat)
      implementation(libs.dev.frontend.androidx.coreKtx)
      implementation(libs.dev.frontend.androidx.activityCompose)

      implementation("io.insert-koin:koin-android")
      implementation("io.insert-koin:koin-androidx-startup")

      implementation(libs.dev.frontend.slf4jAndroid)

    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
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