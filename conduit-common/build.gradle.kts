plugins {
  id("my.kmp-library")
}

group = "mikufan.cx.conduit"

kotlin {
  androidLibrary {
    namespace = "mikufan.cx.conduit.common"
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.dev.datetime)
      }
    }
  }
}