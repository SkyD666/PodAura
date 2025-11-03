import com.android.build.api.variant.FilterConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)       // Compose compiler plugin (AGP+Kotlin manage versions)
    alias(libs.plugins.kotlin.serialization)
    // REMOVE if Android-only: alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.skyd.podaura"
    compileSdk = 36

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
        includeInApk = false
        includeInBundle = false
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    applicationVariants.all {
        outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val abi = output.getFilter(FilterConfiguration.FilterType.ABI.name) ?: "universal"
                output.outputFileName =
                    "PodAura_${versionName}_${abi}_${buildType.name}_${flavorName}.apk"
            }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
        }
        release {
            if (signing != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            applicationIdSuffix = ".benchmark"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes += mutableSetOf(
            "DebugProbesKt.bin",
            "META-INF/CHANGES",
            "META-INF/README.md",
            "META-INF/jdom-info.xml",
            "kotlin-tooling-metadata.json",
            "okhttp3/internal/publicsuffix/NOTICE",
        )
        jniLibs {
            excludes += mutableSetOf(
                "lib/*/libffmpegkit.so",
                "lib/*/libffmpegkit_abidetect.so",
            )
            useLegacyPackaging = true
        }
        dex {
            useLegacyPackaging = true
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

composeCompiler {
    // With modern AGP/Kotlin, the extension aligns automatically; no manual version here.
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
}

tasks.withType(KotlinCompile::class).configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            "-opt-in=kotlin.time.ExperimentalTime",
        )
    }
}

dependencies {
    // ---------- Compose BOM (must be first for compose artifacts) ----------
    implementation(platform(libs.compose.bom))

    // AndroidX core & Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.media)
    implementation(libs.androidx.compose.runtime.tracing) // version managed by BOM
    implementation(libs.android.material)
    implementation(libs.accompanist.permissions)

    // ---------- Compose (AndroidX only; no explicit versions) ----------
    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation + Lifecycle (AndroidX)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ---------- Material3 Adaptive (AndroidX line) ----------
    implementation("androidx.compose.material3.adaptive:adaptive")
    implementation("androidx.compose.material3.adaptive:adaptive-layout")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")

    // AndroidX / Jetpack libs
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    // DI & coroutines & misc
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)

    implementation(libs.kotlinx.coroutines.guava)

    // media & network helpers
    implementation(libs.mpv.lib)
    implementation(libs.ffmpeg.kit)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.coil.video)

    // logging / io
    implementation(libs.kermit)
    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.okio)

    implementation(libs.filekit.core)

    // internal libs
    implementation(libs.skyd666.settings)
    implementation(libs.skyd666.compone)
    implementation(libs.skyd666.mvi)

    implementation(projects.fundation)
    implementation(projects.shared)
    implementation(projects.downloader)

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

fun File.readProperties(): Properties? {
    return if (exists()) {
        Properties().apply {
            this@readProperties.inputStream().use { load(it) }
        }
    } else null
}
