plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        optIn.addAll("com.google.devtools.ksp.KspExperimental")
    }
}

dependencies {
    api(libs.ksp.symbol.processing.api)
    api(libs.androidx.datastore.preferences)
}
