plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.ksp.symbol.processing.api)
    api(libs.androidx.datastore.preferences)
}
