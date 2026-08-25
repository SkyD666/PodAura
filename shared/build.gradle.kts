import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.compose.desktop.application.tasks.AbstractNativeMacApplicationPackageAppDirTask

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

val buildJvmArch = System.getProperty("os.arch").lowercase()
val buildOperatingSystem = System.getProperty("os.name").lowercase()
val macGestureModuleExport =
    "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
val macMediaShimTarget = if (buildOperatingSystem.startsWith("mac")) {
    when (buildJvmArch) {
        "aarch64", "arm64" -> "arm64" to "darwin-aarch64"
        "amd64", "x86_64" -> "x86_64" to "darwin-x86-64"
        else -> error("Unsupported macOS JVM architecture for the media shim: $buildJvmArch")
    }
} else {
    null
}
val macMediaShimSourceDirectory = rootProject.file(
    "fundation/src/jvmMain/objectiveC/macMediaPlayer"
)
val macMediaShimSource = macMediaShimSourceDirectory.resolve("PodAuraMediaPlayer.m")
val macMediaShimBinary = layout.buildDirectory.file(
    "generated/macMediaPlayer/native/libpodaura_media_player.dylib"
)
val macMediaShimJvmResources = layout.buildDirectory.dir(
    "generated/macMediaPlayer/jvmResources"
)
val macMediaShimAppResources = layout.buildDirectory.dir(
    "generated/macMediaPlayer/appResources"
)

val compileMacMediaPlayerShim = macMediaShimTarget?.let { (nativeArchitecture, _) ->
    tasks.register<Exec>("compileMacMediaPlayerShim") {
        inputs.files(
            macMediaShimSourceDirectory.resolve("PodAuraMediaPlayer.h"),
            macMediaShimSource,
        )
        outputs.file(macMediaShimBinary)

        doFirst {
            val outputDirectory = outputs.files.singleFile.parentFile
            check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        }
        commandLine(
            "xcrun",
            "--sdk", "macosx",
            "clang",
            "-arch", nativeArchitecture,
            "-dynamiclib",
            "-fobjc-arc",
            "-fblocks",
            "-fvisibility=hidden",
            "-mmacosx-version-min=11.0",
            "-Wall",
            "-Wextra",
            "-Wl,-install_name,@rpath/libpodaura_media_player.dylib",
            "-I", macMediaShimSourceDirectory.absolutePath,
            macMediaShimSource.absolutePath,
            "-framework", "Foundation",
            "-framework", "AppKit",
            "-framework", "MediaPlayer",
            "-o", macMediaShimBinary.get().asFile.absolutePath,
        )
    }
}

val prepareMacMediaPlayerJvmResources = macMediaShimTarget?.let { (_, resourcePrefix) ->
    val compileTask = requireNotNull(compileMacMediaPlayerShim)
    tasks.register<Sync>("prepareMacMediaPlayerJvmResources") {
        dependsOn(compileTask)
        from(macMediaShimBinary)
        into(macMediaShimJvmResources.map { it.dir(resourcePrefix) })
    }
}

val prepareMacMediaPlayerAppResources = macMediaShimTarget?.let {
    val compileTask = requireNotNull(compileMacMediaPlayerShim)
    tasks.register<Sync>("prepareMacMediaPlayerAppResources") {
        dependsOn(compileTask)
        from(macMediaShimBinary)
        into(macMediaShimAppResources.map { it.dir("macos") })
    }
}

kotlin {

    android {
        namespace = "com.skyd.podaura.shared"
        minSdk = 24
        compileSdk {
            version = release(37) { minorApiLevel = 1 }
        }
        buildToolsVersion = "37.0.0"
        androidResources.enable = true
        withHostTest {}
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

            implementation(libs.zoomimage)

            implementation(libs.okio)

            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
            implementation(libs.xmlutil.serialization.io)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)

            implementation(libs.compottie)
            implementation(libs.kermit)
            implementation(libs.codepoints.deluxe)
            implementation(libs.ksoup)
            implementation(libs.readability)
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
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
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
            implementation(libs.ktor.client.apache5)
            implementation(libs.java.jna)
            implementation(libs.java.jna.platform)
            implementation(libs.java.jaudiotagger)

            implementation(libs.mediamp)

            // DefaultNativePlatform reports the physical CPU under Rosetta. Packaging must follow
            // the JVM that runs Gradle so an x64 JDK produces an entirely x64 distribution.
            when {
                buildOperatingSystem.startsWith("windows") &&
                        buildJvmArch in setOf("amd64", "x86_64") -> {
                    runtimeOnly(libs.mediamp.runtime.windows.x64)
                }
                buildOperatingSystem.startsWith("windows") &&
                        buildJvmArch in setOf("aarch64", "arm64") -> {
                    runtimeOnly(libs.mediamp.runtime.windows.arm64)
                }
                buildOperatingSystem.startsWith("mac") &&
                        buildJvmArch in setOf("amd64", "x86_64") -> {
                    runtimeOnly(libs.mediamp.runtime.macos.x64)
                }
                buildOperatingSystem.startsWith("mac") &&
                        buildJvmArch in setOf("aarch64", "arm64") -> {
                    runtimeOnly(libs.mediamp.runtime.macos.arm64)
                }
                buildOperatingSystem.startsWith("linux") &&
                        buildJvmArch in setOf("amd64", "x86_64") -> {
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
        prepareMacMediaPlayerJvmResources?.let { prepareResources ->
            dependsOn(prepareResources)
            from(macMediaShimJvmResources)
        }
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

// mediamp 0.2.1 accidentally publishes Compose's JUnit UI test stack as a runtime
// dependency. Besides bloating distributions, Truth leaves optional ASM references
// unresolved during desktop ProGuard.
configurations.matching { it.name == "jvmRuntimeClasspath" }.configureEach {
    exclude(group = "org.jetbrains.compose.ui", module = "ui-test-junit4")
}

compose.desktop {
    application {
        mainClass = "com.skyd.podaura.MainKt"
        if (buildOperatingSystem.startsWith("mac")) {
            jvmArgs += macGestureModuleExport
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PodAura"
            packageVersion = findProperty("versionForDesktop")!!.toString()
            appResourcesRootDir.set(macMediaShimAppResources)

            macOS {
                bundleID = "com.skyd.podaura"
                iconFile = project.file("icons/icon_512x512.icns")
                minimumSystemVersion = "11.0"
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

if (buildOperatingSystem.startsWith("mac")) {
    tasks.withType<JavaExec>().configureEach {
        jvmArgs(macGestureModuleExport)
    }
    tasks.withType<Test>().configureEach {
        jvmArgs(macGestureModuleExport)
    }
}

prepareMacMediaPlayerAppResources?.let { prepareResources ->
    tasks.configureEach {
        if (name == "prepareAppResources") {
            dependsOn(prepareResources)
        }
    }
}

// Compose does not propagate nativeApplication.macOS.bundleID to the generated task.
tasks.withType<AbstractNativeMacApplicationPackageAppDirTask>().configureEach {
    bundleID = "com.skyd.podaura"
}

// Distribution's icon
tasks.withType<AbstractJPackageTask>().configureEach {
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
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "mediampVersion",
            value = libs.versions.mediamp.get()
        )
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
