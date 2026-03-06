plugins {
  id("my.kmp-library")
}

group = "mikufan.cx.conduit"

kotlin {
  android {
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
