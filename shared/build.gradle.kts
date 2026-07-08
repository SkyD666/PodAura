import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.buildkonfig)
}

kotlin {

    android {
        namespace = "com.skyd.podaura.shared"
        minSdk = 24
        compileSdk {
            version = release(37) { minorApiLevel = 0 }
        }
        buildToolsVersion = "37.0.0"
        androidResources.enable = true
    }

    jvm()

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

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)

            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.ui.preview)
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
            implementation(libs.jetbrains.navigationevent)

            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
            implementation(libs.androidx.constraintlayout.compose)

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

            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.room3.paging)

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
            implementation(libs.reorderable)
            implementation(libs.skyd666.settings)
            implementation(libs.skyd666.compone)
            implementation(libs.skyd666.mvi)

            implementation(projects.fundation)
            implementation(projects.ksp.annotation)
            implementation(projects.downloader)
            implementation(projects.htmlrender)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.media)
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
            implementation(libs.java.jna.platform)
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes"
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
            "androidx.compose.ui.InternalComposeUiApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.experimental.ExperimentalNativeApi",
            "kotlin.ExperimentalStdlibApi",
            "com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "io.ktor.utils.io.InternalAPI",
            "coil3.annotation.ExperimentalCoilApi"
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
            packageVersion = findProperty("versionForDesktop")!!.toString()

            macOS {
                bundleID = "com.skyd.podaura"
                iconFile = project.file("icons/icon_512x512.icns")
            }
            windows {
                iconFile = project.file("icons/PodAura.ico")
                dirChooser = true
                shortcut = true
                menu = true
                menuGroup = "PodAura"
                // https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "451A428C-D349-458F-8B96-309CAA2F533C"
            }

            modules(
                "jdk.unsupported",
                "java.sql",
            )
        }

        buildTypes.release.proguard {
            version = "7.9.1"
            // obfuscate = true
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
    }
    nativeApplication {
        targets(kotlin.macosArm64())
        distributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "PodAura"
            packageVersion = findProperty("versionForDesktop")!!.toString()

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
        add(it, libs.androidx.room3.compiler)
    }
}

buildkonfig {
    packageName = "com.skyd.podaura"

    defaultConfigs {
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "packageName",
            value = "com.skyd.podaura"
        )
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "versionName",
            value = findProperty("versionName")!!.toString()
        )
        buildConfigField(
            type = FieldSpec.Type.INT,
            name = "versionCode",
            value = findProperty("versionCode")!!.toString()
        )
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "versionForDesktop",
            value = findProperty("versionForDesktop")!!.toString()
        )
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
