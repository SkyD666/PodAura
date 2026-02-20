@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.FilterConfiguration
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.skyd.podaura"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    buildToolsVersion = "36.1.0"
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.skyd.anivu"
        minSdk = 24
        targetSdk = 36
        versionCode = properties["versionCode"]!!.toString().toInt()
        versionName = properties["versionName"]!!.toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val signing = rootProject.file("signing.properties").readProperties()

    signingConfigs {
        if (signing != null) {
            create("release") {
                storeFile = rootProject.file(signing.getProperty("KEYSTORE_FILE"))
                storePassword = signing.getProperty("KEYSTORE_PASSWORD")
                keyAlias = signing.getProperty("KEY_ALIAS")
                keyPassword = signing.getProperty("KEY_PASSWORD")
            }
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("GitHub") {
            dimension = "version"
        }
    }

    // https://github.com/SkyD666/PodAura/issues/59#issuecomment-2597764128
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    splits {
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = true
            // By default, all ABIs are included, so use reset() and include().
            // Resets the list of ABIs for Gradle to create APKs for to none.
            reset()
            // A list of ABIs for Gradle to create APKs for.
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            // We want to also generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            if (signing != null) {
                signingConfig = signingConfigs.getByName("release")    // signing
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += "release"
            isDebuggable = false
            applicationIdSuffix = ".benchmark"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += mutableSetOf(
            "DebugProbesKt.bin",
            "META-INF/*.version",
            "META-INF/**/LICENSE.txt",
            "META-INF/native-image/**"
        )
        jniLibs {
            excludes += mutableSetOf(
                "lib/*/libffmpegkit.so",                // mpv-android
                "lib/*/libffmpegkit_abidetect.so",      // mpv-android
            )
            useLegacyPackaging = true
        }
        dex {
            useLegacyPackaging = true
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    lint.checkReleaseBuilds = false
}

androidComponents {
    onVariants { variant ->
        variant.outputs
            .map { it as VariantOutputImpl }
            .forEach { output ->
                val versionName = properties["versionName"]
                val abi = output.getFilter(FilterConfiguration.FilterType.ABI)?.identifier ?: "universal"
                val buildType = variant.buildType
                val flavorName = variant.flavorName
                output.outputFileName = "PodAura_${versionName}_${abi}_${buildType}_${flavorName}.apk"
            }
    }
}

// https://stackoverflow.com/a/77745844
tasks.withType<PackageAndroidArtifact> {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xskip-prerelease-check",
        )
        jvmTarget = JvmTarget.JVM_21
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.ExperimentalStdlibApi",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.time.ExperimentalTime"
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
//    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.media)
    implementation(libs.androidx.compose.runtime.tracing)

    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.ui)
    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.material3.window.size)
    implementation(libs.jetbrains.compose.material3.adaptive)
    implementation(libs.jetbrains.compose.material3.adaptive.layout)
    implementation(libs.jetbrains.compose.materialIconsExtended)
    implementation(libs.jetbrains.compose.components.resources)
    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.jetbrains.lifecycle.runtime.compose)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)

    implementation(libs.kotlinx.coroutines.guava)

    implementation(libs.mpv.lib)
    implementation(libs.ffmpeg.kit)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.coil.video)

    implementation(libs.kermit)
    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.okio)

    implementation(libs.filekit.core)

    implementation(libs.skyd666.settings)
    implementation(libs.skyd666.compone)
    implementation(libs.skyd666.mvi)

    implementation(projects.fundation)
    implementation(projects.shared)
    implementation(projects.downloader)

//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.paging.test)
    androidTestImplementation(libs.androidx.work.test)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.uiautomator)
}

fun File.readProperties(): Properties? =
    takeIf { exists() }?.inputStream()?.use { stream ->
        Properties().apply { load(stream) }
    }
