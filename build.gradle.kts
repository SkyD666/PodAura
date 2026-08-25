// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.buildkonfig) apply false
}

subprojects {
    configurations.configureEach {
        setOf(
            "desktop-jvm-linux-x64",
            "desktop-jvm-linux-arm64",
            "desktop-jvm-windows-x64",
            "desktop-jvm-windows-arm64",
            "desktop-jvm-macos-x64",
            "desktop-jvm-macos-arm64",
        ).forEach { module ->
            exclude(group = "org.jetbrains.compose.desktop", module = module)
        }
    }
}

tasks.withType<UpdateDaemonJvm>().configureEach {
    languageVersion = JavaLanguageVersion.of(25)
    vendor = JvmVendorSpec.AZUL
}
