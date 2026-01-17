import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.buildkonfig)
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.skyd.fundation"
        minSdk = 24
        compileSdk {
            version = release(36) { minorApiLevel = 1 }
        }
        buildToolsVersion = "36.1.0"
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
/*
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FundationKit"
            isStatic = true
        }
    }
 */

    jvm()

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
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
            implementation(projects.ksp)
        }

        jvmMain.dependencies {
            implementation(libs.java.jna)
        }
    }

    compilerOptions {
        optIn.addAll("kotlin.time.ExperimentalTime")
    }
}

buildkonfig {
    packageName = "com.skyd.fundation"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "packageName", "com.skyd.podaura")
        buildConfigField(FieldSpec.Type.STRING, "versionName", properties["versionName"]!!.toString())
        buildConfigField(FieldSpec.Type.INT, "versionCode", properties["versionCode"]!!.toString())
    }
}

//dependencies {
//    listOf("kspCommonMainMetadata", "kspAndroid", "kspJvm").forEach {
//        add(it, projects.ksp)
//    }
//}
