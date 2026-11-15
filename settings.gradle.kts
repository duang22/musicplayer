pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Use Google's Maven Central mirror to avoid HTTP 403 from the default endpoint.
        maven {
            url = uri("https://maven-central.storage-download.googleapis.com/maven2")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Keep plugin and library dependency resolution on the same mirror.
        maven {
            url = uri("https://maven-central.storage-download.googleapis.com/maven2")
        }
        mavenCentral()
    }
}

rootProject.name = "MusicPlayer"
include(":app")
 
