plugins {
  id("my.kmp-library")
}

group = "mikufan.cx.conduit"

kotlin {
  android {
    namespace = "mikufan.cx.conduit.common"
  }
  sourceSets {
    commonMain.dependencies {
      implementation(libs.dev.datetime)
    }
  }
}
