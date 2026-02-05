enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    repositories {
        google()
//        maven(url = "https://maven.aliyun.com/repository/public")
        mavenCentral()
        maven(url = "https://jitpack.io")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
//        maven(url = "https://maven.aliyun.com/repository/public")
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "PodAura"
include(
    ":android:app",
    ":android:benchmark",
    ":downloader",
    ":ksp",
    ":shared",
    ":fundation",
    ":htmlrender"
)
