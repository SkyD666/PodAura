import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.skyd.downloader"
        minSdk = 24
        compileSdk {
            version = release(36) { minorApiLevel = 1 }
        }
        buildToolsVersion = "36.1.0"
        androidResources.enable = true
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
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "DownloaderKit"
            isStatic = true
        }
    }

    jvm()

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.filekit.core)
            implementation(libs.ktor.client.core)
            implementation(libs.koin.core)
            implementation(libs.kermit)
            implementation(libs.kotlincrypto.hash.md)
            implementation(libs.skyd666.compone)

            implementation(projects.fundation)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.work.runtime.ktx)
        }

//        iosMain {
//            dependencies {
//                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
//                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
//                // part of KMP’s default source set hierarchy. Note that this source set depends
//                // on common by default and will correctly pull the iOS artifacts of any
//                // KMP dependencies declared in commonMain.
//            }
//        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll(
            "kotlin.time.ExperimentalTime",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.concurrent.atomics.ExperimentalAtomicApi"
        )
    }
}

dependencies {
    listOf("kspCommonMainMetadata", "kspAndroid", "kspJvm").forEach {
        add(it, projects.ksp)
        if (it != "kspCommonMainMetadata") {
            add(it, libs.androidx.room.compiler)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    publicResClass = true
}
