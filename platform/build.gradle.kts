import com.skyd.podaura.buildlogic.addWindowsMediaFileAssociations
import com.skyd.podaura.buildlogic.addWindowsNotificationProtocol
import com.skyd.podaura.buildlogic.configureWindowsMsiOpenWith
import com.skyd.podaura.buildlogic.macOSAppLocalizations
import com.skyd.podaura.buildlogic.macOSMediaDocumentTypes
import com.skyd.podaura.buildlogic.macOSNotificationUrlTypes
import com.skyd.podaura.buildlogic.windowsMediaFileAssociations
import de.stefan_oltmann.msix.CreateAppxManifestTask
import de.stefan_oltmann.msix.CreateMsixIconsTask
import de.stefan_oltmann.msix.CreateMsixTask
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.gradle.msix)
    id("podaura.macos-application")
    id("podaura.apple-mpv-link") apply (System.getProperty("os.name").startsWith("Mac", true))
}

val macGestureModuleExport = "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
val desktopMediaShimAppResources = project(":shared").layout.buildDirectory.dir(
    "generated/desktopMediaPlayer/appResources"
)

kotlin {
    android {
        namespace = "com.skyd.podaura.platform"
        minSdk = 24
        compileSdk { version = release(37) { minorApiLevel = 2 } }
        buildToolsVersion = "37.0.0"
        androidResources.enable = true
    }
    jvm()
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    macosArm64 {
        binaries.executable { entryPoint = "com.skyd.podaura.app.main" }
    }
    sourceSets {
        // Keep plugin-generated sources when adding the platform-specific directories.
        commonMain {
            kotlin.srcDir("common/src/main/kotlin")
            resources.srcDir("common/src/main/resources")
            dependencies {
                implementation(projects.shared)
                implementation(projects.sharedApi)
                implementation(projects.podnya)
                implementation(projects.fundation)
                implementation(libs.androidx.navigation3.runtime)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.skyd666.compone)
                implementation(libs.koin.core)
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.components.resources)
            }
        }
        jvmMain {
            kotlin.srcDir("jvm/src/main/kotlin")
            resources.srcDir("jvm/src/main/resources")
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.filekit.core)
            }
        }
        iosMain {
            kotlin.srcDir("ios/src/main/kotlin")
            resources.srcDir("ios/src/main/resources")
        }
        macosMain {
            kotlin.srcDir("macos/src/main/kotlin")
            resources.srcDir("macos/src/main/resources")
        }
        jvmTest {
            kotlin.srcDir("jvm/src/test/kotlin")
            resources.srcDir("jvm/src/test/resources")
            dependencies {
                implementation(libs.jetbrains.compose.ui.test.junit4)
                implementation(projects.sharedApi)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.androidx.datastore.preferences)
            }
        }
    }
}

// mediamp publishes UI test libraries at runtime; keep them out of desktop distributions.
configurations.matching { it.name == "jvmRuntimeClasspath" }.configureEach {
    exclude(group = "org.jetbrains.compose.ui", module = "ui-test-junit4")
}

tasks.matching { it.name == "prepareAppResources" }.configureEach {
    val os = System.getProperty("os.name")
    if (os.startsWith("Mac", true)) dependsOn(":shared:prepareMacMediaPlayerAppResources")
    val arch = System.getProperty("os.arch").lowercase()
    if (os.startsWith("Windows", true) && arch in setOf("amd64", "x86_64")) {
        dependsOn(":shared:prepareWindowsMediaPlayerAppResources")
    }
}

compose.desktop {
    application {
        mainClass = "com.skyd.podaura.app.MainKt"
        if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
            jvmArgs += macGestureModuleExport
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PodAura"
            packageVersion = findProperty("versionForDesktop")!!.toString()
            appResourcesRootDir = desktopMediaShimAppResources

            macOS {
                bundleID = "com.skyd.podaura"
                iconFile = rootProject.file("shared/icons/icon_512x512.icns")
                infoPlist {
                    extraKeysRawXml =
                        macOSMediaDocumentTypes() + macOSNotificationUrlTypes() + macOSAppLocalizations()
                }
            }
            windows {
                iconFile = rootProject.file("shared/icons/PodAura.ico")
                dirChooser = true
                shortcut = true
                menu = true
                menuGroup = "PodAura"
                windowsMediaFileAssociations().forEach { (extension, mimeType) ->
                    fileAssociation(
                        mimeType = mimeType,
                        extension = extension,
                        description = "PodAura media file",
                        iconFile = rootProject.file("shared/icons/PodAura.ico"),
                    )
                }
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
            configurationFiles.from(
                rootProject.file("shared/compose-desktop.pro"),
                rootProject.file("podnya/compose-desktop.pro"),
            )
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
                iconFile = rootProject.file("shared/icons/PodAura.icns")
            }
        }
    }
}

// Native executable runs need Compose resources beside the .kexe, outside an app bundle.
listOf("Debug", "Release").forEach { buildType ->
    val linkTask = tasks.named("link${buildType}ExecutableMacosArm64")
    val copyResourcesTask = tasks.register<Sync>(
        "copyMacosArm64ResourcesTo${buildType}Executable"
    ) {
        description = "Copies Compose resources beside the macOS $buildType executable."
        dependsOn("macosArm64AggregateResources")
        mustRunAfter(linkTask)
        from(layout.buildDirectory.dir("kotlin-multiplatform-resources/aggregated-resources/macosArm64"))
        into(layout.buildDirectory.dir("bin/macosArm64/${buildType.lowercase()}Executable/compose-resources"))
    }
    linkTask.configure {
        finalizedBy(copyResourcesTask)
    }
    tasks.named("run${buildType}ExecutableMacosArm64") {
        dependsOn(copyResourcesTask)
    }
}

msix {
    svgIcon = rootProject.layout.projectDirectory.file("doc/image/PodAura.svg")

    manifest {
        appId = "PodAura"
        displayName = "PodAura"
        description =
            "An all-in-one Podcast app for RSS subscriptions, updates, media downloads and playback."
        identityName = "SkyD666.PodAura"
        publisher = "CN=A899BB3F-B2EE-4733-BFE7-45715FA85273"
        publisherDisplayName = "SkyD666"
        version = "${findProperty("versionForDesktop")}.0"
        processorArchitecture = "x64"
        appExecutable = "PodAura.exe"
        targetDeviceFamilyMinVersion = "10.0.17763.0"
    }
}

// gradle-msix-plugin 0.2.1 cannot discover the package name from Compose 1.12.
val msixApplicationDirectory =
    layout.buildDirectory.dir("compose/binaries/main-release/app/PodAura")

tasks.named<CreateMsixIconsTask>("createMsixIcons").configure {
    outputDir = msixApplicationDirectory.map { it.dir("resources") }
}
tasks.named<CreateAppxManifestTask>("createAppxManifest") {
    outputFile = msixApplicationDirectory.map { it.file("AppxManifest.xml") }
    inputs.property("mediaFileAssociations", windowsMediaFileAssociations())
    inputs.property("notificationProtocol", "podaura")
    doLast {
        addWindowsMediaFileAssociations(outputFile.get().asFile)
        addWindowsNotificationProtocol(outputFile.get().asFile)
    }
}
tasks.named<CreateMsixTask>("createMsix") {
    appDirectory = msixApplicationDirectory
    msixOutputFile = layout.buildDirectory.file("PodAura.msix")
}

if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
    tasks.withType<JavaExec>().configureEach {
        jvmArgs(macGestureModuleExport)
    }
    tasks.withType<Test>().configureEach {
        jvmArgs(macGestureModuleExport)
    }
}

tasks.withType<AbstractJPackageTask>().configureEach {
    if (targetFormat == TargetFormat.Dmg) {
        // Distribution's icon
        freeArgs.addAll("--icon", rootProject.file("shared/icons/icon_512x512.icns").absolutePath)
    }
    if (targetFormat == TargetFormat.Msi) {
        inputs.property("openWithExtensions", windowsMediaFileAssociations().keys)
        doLast {
            configureWindowsMsiOpenWith(
                destinationDir.get().asFile.resolve("${packageName.get()}-${packageVersion.get()}.msi"),
                temporaryDir,
            )
        }
    }
}
