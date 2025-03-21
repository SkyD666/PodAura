pluginManagement {
    repositories {
        google()
//        maven(url = "https://maven.aliyun.com/repository/public")
        mavenCentral()
        maven(url = "https://jitpack.io")
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
//        maven(url = "https://maven.aliyun.com/repository/public")
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "PodAura"
include(":app")
include(":downloader")
include(":benchmark")
