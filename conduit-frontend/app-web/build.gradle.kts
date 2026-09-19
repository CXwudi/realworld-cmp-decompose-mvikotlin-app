import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  id("my.cmp-app")
}

kotlin {
  js {
    browser {}
    binaries.executable()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser {}
    binaries.executable()
  }

  sourceSets {
    commonMain.dependencies {
      implementation(project(":frontend-logic"))
      implementation(project(":frontend-compose-ui"))

      implementation(project.dependencies.platform(libs.dev.frontend.kotlinWrapper))
      implementation(libs.dev.frontend.kotlinxBrowser)
      implementation("org.jetbrains.kotlin-wrappers:kotlin-browser")
    }
  }
}
