import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.buildkonfig)
}

kotlin {

    android {
        namespace = "com.skyd.fundation"
        minSdk = 24
        compileSdk {
            version = release(37) { minorApiLevel = 0 }
        }
        buildToolsVersion = "37.0.0"
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "FundationKit"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.core)
            implementation(libs.kermit)
        }

        jvmMain.dependencies {
            implementation(libs.java.jna)
            implementation(libs.java.jna.platform)
            implementation(libs.ocpsoft.prettytime)
            implementation(libs.icu4j)
        }
    }

    compilerOptions {
        optIn.addAll(
            "kotlin.time.ExperimentalTime",
            "kotlinx.cinterop.ExperimentalForeignApi"
        )
    }
}

buildkonfig {
    packageName = "com.skyd.fundation"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "packageName", "com.skyd.podaura")
        buildConfigField(FieldSpec.Type.STRING, "versionName", findProperty("versionName")!!.toString())
        buildConfigField(FieldSpec.Type.INT, "versionCode", findProperty("versionCode")!!.toString())
    }
}
