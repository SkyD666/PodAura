plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        optIn.addAll("com.google.devtools.ksp.KspExperimental")
    }
}

dependencies {
    implementation(projects.ksp.annotation)
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.androidx.datastore.preferences)
}
