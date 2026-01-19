package my

import my.util.Libs

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.compose")
  kotlin("plugin.compose")
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      // coroutine
      implementation(project.dependencies.platform(Libs.CoroutinesBom))
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
      // compose
      implementation(Libs.ComposeRuntime)
      implementation(Libs.ComposeUi)
      implementation(Libs.ComposeFoundation)
      implementation(Libs.ComposeMaterial3)
      implementation(Libs.ComposeResources)
      // decompose + mvikotlin
      implementation(Libs.Decompose)
      implementation(Libs.DecomposeCompose)
      implementation(Libs.KotlinLogging)
      // koin
      implementation(project.dependencies.platform(Libs.KoinBom))
      implementation("io.insert-koin:koin-core")
    }
  }
}