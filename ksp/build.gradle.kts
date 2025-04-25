plugins {
    kotlin("multiplatform")
}
kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(libs.symbol.processing.api)
                implementation(libs.androidx.datastore.preferences)
            }
            kotlin.srcDir("src/main/java")
            resources.srcDir("src/main/resources")
        }
    }
}