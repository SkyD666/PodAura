import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.skyd.fundation"
        compileSdk = 36
        minSdk = 24
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
//    val xcfName = "fundationKit"
//
//    iosX64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }
//
//    iosArm64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }
//
//    iosSimulatorArm64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }

    jvm()

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.io.core)
                implementation(compose.runtime)
                implementation(compose.components.resources)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.koin.core)
                implementation(libs.kermit)
                implementation(projects.ksp)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
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

        all {
            with(languageSettings) {
                optIn("kotlin.time.ExperimentalTime")
            }
        }
    }
}

//dependencies {
//    listOf("kspCommonMainMetadata", "kspAndroid", "kspJvm").forEach {
//        add(it, projects.ksp)
//    }
//}
//
//// Trigger Common Metadata Generation from Native tasks
//project.tasks.withType(KspAATask::class).configureEach {
//    if (name != "kspCommonMainKotlinMetadata") {
//        dependsOn("kspCommonMainKotlinMetadata")
//    }
//}