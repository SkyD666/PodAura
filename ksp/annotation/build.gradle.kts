import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.skyd.ksp.annotation"
        minSdk = 24
        compileSdk {
            version = release(37) { minorApiLevel = 0 }
        }
        buildToolsVersion = "37.0.0"
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "KspAnnotation"
            isStatic = true
        }
    }

    jvm()
}
