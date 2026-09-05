plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir("../shared/media-types")
}

dependencies {
    testImplementation(kotlin("test"))
}
