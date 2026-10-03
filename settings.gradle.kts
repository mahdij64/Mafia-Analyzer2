pluginManagement {
  repositories {
    google {
      url = uri("https://dl.google.com/dl/android/maven2/")
    }
    mavenCentral()
    gradlePluginPortal()
  }
  plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
    id("com.google.gms.google-services") version "4.5.0"
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google {
      url = uri("https://dl.google.com/dl/android/maven2/")
    }
    mavenCentral()
  }
}

rootProject.name = "Mafia Analyzer"

include(":app")
