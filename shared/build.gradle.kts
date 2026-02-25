import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.buildkonfig)
}

kotlin {

// Target declarations - add or remove as needed below. These define
// which platforms this KMP module supports.
// See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.skyd.podaura.shared"
        minSdk = 24
        compileSdk {
            version = release(36) { minorApiLevel = 1 }
        }
        buildToolsVersion = "36.1.0"
        androidResources.enable = true
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    jvm()

// For iOS targets, this is also where you should
// configure native binary output. For more information, see:
// https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

// A step-by-step guide on how to include this library in an XCode
// project can be found here:
// https://developer.android.com/kotlin/multiplatform/migrate
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    macosArm64 {
        binaries.executable {
            entryPoint = "com.skyd.podaura.main"
        }
    }

// Source set declarations.
// Declaring a target automatically creates a source set with the same name. By default, the
// Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
// common to share sources between related targets.
// See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)

            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.ui.preview)
            implementation(libs.jetbrains.compose.ui.backhandler)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.material3.window.size)
            implementation(libs.jetbrains.compose.material3.adaptive)
            implementation(libs.jetbrains.compose.material3.adaptive.layout)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation3)
            implementation(libs.jetbrains.compose.materialIconsExtended)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigation3.ui)

            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
            implementation(libs.androidx.constraintlayout.compose)
            implementation(libs.androidx.navigation3.runtime)

            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.navigation3)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.serialization.kotlinx.xml)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.coil.svg)

            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
            implementation(libs.xmlutil.serialization.io)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)

            implementation(libs.compottie)
            implementation(libs.kermit)
            implementation(libs.codepoints.deluxe)
            implementation(libs.ksoup)
            implementation(libs.material.kolor)
            // implementation(libs.reorderable)
            implementation(libs.skyd666.settings)
            implementation(libs.skyd666.compone)
            implementation(libs.skyd666.mvi)

            implementation(projects.fundation)
            implementation(projects.ksp.annotation)
            implementation(projects.downloader)
            implementation(projects.htmlrender)
            implementation(projects.reorderable)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.media)
            implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.graphics.shapes)

            implementation(libs.accompanist.permissions)

            implementation(libs.ktor.client.okhttp)

            implementation(libs.coil.gif)
            implementation(libs.coil.video)
        }

        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.jetbrains.compose.desktop.common)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.ktor.client.apache5)
            implementation(libs.java.jna)
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xexplicit-backing-fields"
        )
        optIn.addAll(
            "org.jetbrains.compose.resources.ExperimentalResourceApi",
            "org.jetbrains.compose.resources.InternalResourceApi",
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
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.ExperimentalStdlibApi",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.time.ExperimentalTime",
            "com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi"
        )
    }

    // KSP Common sourceSet
    sourceSets.commonMain.configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

compose.resources {
    publicResClass = true
}

compose.desktop {
    application {
        mainClass = "com.skyd.podaura.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PodAura"
            packageVersion = properties["versionForDesktop"]!!.toString()

            macOS {
                bundleID = "com.skyd.podaura"
                iconFile = project.file("icons/icon_512x512.icns")
            }
            windows {
                iconFile = project.file("icons/icon.ico")
                dirChooser = true
            }

            modules(
                "jdk.unsupported",
                "java.sql",
            )
        }

        buildTypes.release.proguard {
//            obfuscate = true
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
    }
    nativeApplication {
        targets(kotlin.macosArm64())
        distributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "PodAura"
            packageVersion = properties["versionForDesktop"]!!.toString()

            macOS {
                bundleID = "com.skyd.podaura"
                // https://github.com/JetBrains/compose-multiplatform/blob/e68123684b732adb34a5fb3704c9de868bdbed0e/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/desktop/application/tasks/AbstractNativeMacApplicationPackageAppDirTask.kt#L63-L64
                // The icon file in Contents/Resources has been hardcoded to "$packageName.icns".
                iconFile = project.file("icons/PodAura.icns")
            }
        }
    }
}

// Distribution's icon
tasks.withType<AbstractJPackageTask> {
    if (targetFormat == TargetFormat.Dmg) {
        freeArgs.addAll("--icon", "icons/icon_512x512.icns")
    }
}

dependencies {
    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64", "kspMacosArm64").forEach {
        add(it, projects.ksp.processor)
        add(it, libs.androidx.room.compiler)
    }
}

buildkonfig {
    packageName = "com.skyd.podaura"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "packageName", "com.skyd.podaura")
        buildConfigField(
            FieldSpec.Type.STRING,
            "versionName",
            properties["versionName"]!!.toString()
        )
        buildConfigField(FieldSpec.Type.INT, "versionCode", properties["versionCode"]!!.toString())
        buildConfigField(
            FieldSpec.Type.STRING,
            "versionForDesktop",
            properties["versionForDesktop"]!!.toString()
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}
