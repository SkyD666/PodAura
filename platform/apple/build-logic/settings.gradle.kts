rootProject.name = "podaura-apple-build-logic"

dependencyResolutionManagement {
    repositories { gradlePluginPortal() }
    versionCatalogs {
        create("libs") { from(files("../../../gradle/libs.versions.toml")) }
    }
}
