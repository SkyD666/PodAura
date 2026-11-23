import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

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
    id("podaura.desktop-media-shims")
    id("podaura.apple-mpv") apply (System.getProperty("os.name").startsWith("Mac", true))
    id("podaura.macos-appkit")
}

val isMicrosoftStoreBuild = providers.gradleProperty("microsoftStore")
    .map { it.toBooleanStrict() }
    .getOrElse(false)
val macGestureModuleExport =
    "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"

kotlin {

    android {
        namespace = "com.skyd.podaura.shared"
        minSdk = 24
        compileSdk {
            version = release(37) { minorApiLevel = 2 }
        }
        buildToolsVersion = "37.0.0"
        androidResources.enable = true
        withHostTest {}
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    sourceSets {
        commonMain { kotlin.srcDir("media-types") }
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

            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.room3.paging)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.coil.svg)

            implementation(libs.zoomimage)

            implementation(libs.okio)

            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
            implementation(libs.xmlutil.serialization.io)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)

            implementation(libs.kermit)
            implementation(libs.kermit.ktor)
            implementation(libs.kermit.coil)

            implementation(libs.compottie)
            implementation(libs.codepoints.deluxe)
            implementation(libs.ksoup)
            implementation(libs.readability)
            implementation(libs.material.kolor)
            implementation(libs.reorderable)
            implementation(libs.skyd666.settings)
            implementation(libs.skyd666.compone)
            implementation(libs.skyd666.mvi)

            implementation(projects.sharedApi)
            implementation(projects.fundation)
            implementation(projects.ksp.annotation)
            implementation(projects.downloader)
            implementation(projects.htmlrender)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.media)
            implementation(libs.androidx.graphics.shapes)
            implementation(libs.androidx.webkit)

            implementation(libs.ktor.client.okhttp)

            implementation(libs.coil.gif)
            implementation(libs.coil.video)

            implementation(libs.mpv.lib)
        }

        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.jetbrains.compose.desktop.common)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.java.jna)
            implementation(libs.java.jna.platform)
            implementation(libs.java.jaudiotagger)

            implementation(libs.mediamp)

            // DefaultNativePlatform reports the physical CPU under Rosetta. Packaging must follow
            // the JVM that runs Gradle so a x64 JDK produces an entirely x64 distribution.
            val currentArch = Architectures.forInput(System.getProperty("os.arch"))
            val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
            when {
                currentOs.isWindows && currentArch.isAmd64 -> {
                    runtimeOnly(libs.mediamp.runtime.windows.x64)
                }

                currentOs.isWindows && currentArch.isArm64 -> {
                    runtimeOnly(libs.mediamp.runtime.windows.arm64)
                }

                currentOs.isMacOsX && currentArch.isAmd64 -> {
                    runtimeOnly(libs.mediamp.runtime.macos.x64)
                }

                currentOs.isMacOsX && currentArch.isArm64 -> {
                    runtimeOnly(libs.mediamp.runtime.macos.arm64)
                }

                currentOs.isLinux && currentArch.isAmd64 -> {
                    runtimeOnly(libs.mediamp.runtime.linux.x64)
                }
            }
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.compose.ui.test.junit4)
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
            "androidx.compose.ui.text.ExperimentalTextApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlinx.cinterop.BetaInteropApi",
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.experimental.ExperimentalNativeApi",
            "kotlin.ExperimentalStdlibApi",
            "io.ktor.utils.io.InternalAPI",
            "coil3.annotation.ExperimentalCoilApi"
        )
    }

    // KSP Common sourceSet
    sourceSets.commonMain.configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

tasks.withType<ProcessResources>().configureEach {
    if (name == "jvmProcessResources") {
        from(project.file("icons/PodAura.ico"))
    }
}

compose.resources {
    publicResClass = true
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler/reports")
    metricsDestination = layout.buildDirectory.dir("compose_compiler/metrics")
}

// mediamp 0.5.0 accidentally publishes Compose's JUnit UI test stack as a runtime
// dependency. Besides bloating distributions, Truth leaves optional ASM references
// unresolved during desktop ProGuard.
configurations.matching { it.name == "jvmRuntimeClasspath" }.configureEach {
    exclude(group = "org.jetbrains.compose.ui", module = "ui-test-junit4")
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
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "mediampVersion",
            value = libs.versions.mediamp.get()
        )
        buildConfigField(
            type = FieldSpec.Type.BOOLEAN,
            name = "isMicrosoftStore",
            value = isMicrosoftStoreBuild.toString()
        )
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
    tasks.withType<Test>().configureEach { jvmArgs(macGestureModuleExport) }
}
