plugins {
  id("my.cmp-app")
}

kotlin {
  jvm()
  sourceSets {
    jvmMain {
      dependencies {
        implementation(project(":frontend-logic"))
        implementation(project(":frontend-compose-ui"))
        implementation(compose.desktop.currentOs)
      }
    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
}

compose.desktop {
  application {
    mainClass = "mikufan.cx.conduit.frontend.app.desktop.MainKt"
  }
}
