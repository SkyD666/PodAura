import groovy.json.JsonSlurper
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

// Disabled Kotlin link tasks still execute dependencies, so guard preparation tasks too.
// MPVKit download, C interop and native test linking. Targets/frameworks remain owned by the module.
val prepareIosMpv = tasks.register<Exec>("prepareIosMpv") {
    enabled = HostManager.hostIsMac
    val script = rootProject.file("platform/apple/mpvkit/prepare.py")
    inputs.files(script, rootProject.file("platform/apple/mpvkit/artifacts.py"),
        rootProject.file("platform/apple/mpvkit/artifacts.json"),
        rootProject.file("platform/apple/mpvkit/podaura.h"))
    // Gradle removes stale outputs on a fresh checkout; it must never own the native binaries here.
    outputs.dirs(layout.buildDirectory.dir("mpvkit/iphoneos/Libmpv.framework/Headers"),
        layout.buildDirectory.dir("mpvkit/iphonesimulator/Libmpv.framework/Headers"))
    commandLine("python3", script.absolutePath, "--platform", "ios", "--headers-only")
}

val prepareIosMpvRuntime = tasks.register<Exec>("prepareIosMpvRuntime") {
    enabled = HostManager.hostIsMac
    dependsOn(prepareIosMpv)
    commandLine("python3", rootProject.file("platform/apple/mpvkit/prepare.py").absolutePath,
        "--platform", "ios")
}

val downloadAppleMpvCertificates = tasks.register<Exec>("downloadAppleMpvCertificates") {
    val script = rootProject.file("platform/apple/mpvkit/prepare.py")
    inputs.files(script, rootProject.file("platform/apple/mpvkit/artifacts.py"),
        rootProject.file("platform/apple/mpvkit/artifacts.json"))
    outputs.file(layout.buildDirectory.file("mpvkit/downloads/cacert.pem"))
    commandLine("python3", script.absolutePath, "--cert-only")
}

val prepareIosMpvResources = tasks.register<Sync>("prepareIosMpvResources") {
    from("src/iosMain/composeResources")
    from(downloadAppleMpvCertificates) { into("files") }
    into(layout.buildDirectory.dir("generated/iosMpvResources"))
}

extensions.configure<ComposeExtension> {
    extensions.configure<ResourcesExtension> {
        customDirectory("iosMain", layout.dir(prepareIosMpvResources.map { it.destinationDir }))
    }
}

val artifacts = JsonSlurper().parse(rootProject.file("platform/apple/mpvkit/artifacts.json")) as Map<*, *>
val mpvFrameworks = (artifacts["artifacts"] as List<*>).map {
    (it as Map<*, *>)["name"].toString()
}
val systemFrameworks = listOf(
    "AVFoundation", "AVKit", "AudioToolbox", "CoreAudio", "CoreVideo", "CoreMedia",
    "Metal", "IOSurface", "VideoToolbox", "QuartzCore",
)

extensions.configure<KotlinMultiplatformExtension> {
    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family != Family.IOS) return@configureEach
        val sdk = if (konanTarget.name == "ios_arm64") "iphoneos" else "iphonesimulator"
        val interop = compilations.getByName("main").cinterops.create("mpv") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/mpv.def"))
            includeDirs(layout.buildDirectory.dir("mpvkit/$sdk/Libmpv.framework/Headers"))
        }
        tasks.named(interop.interopProcessingTaskName).configure {
            dependsOn(prepareIosMpv)
            // Runtime extraction replaces the headers read by cinterop.
            mustRunAfter(prepareIosMpvRuntime)
        }
        binaries.withType<TestExecutable>().configureEach {
            linkerOpts("-F${layout.buildDirectory.dir("mpvkit/$sdk").get().asFile}")
            (mpvFrameworks + systemFrameworks).forEach { linkerOpts("-framework", it) }
            linkerOpts("-lbz2", "-liconv", "-lexpat", "-lresolv", "-lxml2", "-lz", "-lc++")
            linkTaskProvider.configure { dependsOn(prepareIosMpvRuntime) }
        }
    }
}

// Native macOS consumes the same pinned dependencies, using their macOS slices.
val prepareMacosMpv = tasks.register<Exec>("prepareMacosMpv") {
    enabled = HostManager.hostIsMac
    val script = rootProject.file("platform/apple/mpvkit/prepare.py")
    inputs.files(script, rootProject.file("platform/apple/mpvkit/artifacts.py"),
        rootProject.file("platform/apple/mpvkit/artifacts.json"),
        rootProject.file("platform/apple/mpvkit/podaura.h"))
    outputs.dir(layout.buildDirectory.dir("mpvkit/macos/Libmpv.framework/Headers"))
    commandLine("python3", script.absolutePath, "--platform", "macos", "--headers-only")
}
val prepareMacosMpvRuntime = tasks.register<Exec>("prepareMacosMpvRuntime") {
    enabled = HostManager.hostIsMac
    dependsOn(prepareMacosMpv)
    commandLine("python3", rootProject.file("platform/apple/mpvkit/prepare.py").absolutePath,
        "--platform", "macos")
}
val prepareMacosMpvResources = tasks.register<Sync>("prepareMacosMpvResources") {
    from(downloadAppleMpvCertificates) { into("files") }
    into(layout.buildDirectory.dir("generated/macosMpvResources"))
}
extensions.configure<ComposeExtension> {
    extensions.configure<ResourcesExtension> {
        customDirectory("macosMain", layout.dir(prepareMacosMpvResources.map { it.destinationDir }))
    }
}
extensions.configure<KotlinMultiplatformExtension> {
    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family != Family.OSX) return@configureEach
        val interop = compilations.getByName("main").cinterops.create("mpv") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/mpv.def"))
            includeDirs(layout.buildDirectory.dir("mpvkit/macos/Libmpv.framework/Headers"))
        }
        tasks.named(interop.interopProcessingTaskName).configure {
            dependsOn(prepareMacosMpv)
            // Runtime extraction replaces the headers read by cinterop.
            mustRunAfter(prepareMacosMpvRuntime)
        }
        binaries.configureEach {
            linkerOpts("-F${layout.buildDirectory.dir("mpvkit/macos").get().asFile}")
            (mpvFrameworks + systemFrameworks + listOf("AppKit", "OpenGL", "IOKit", "CoreFoundation", "Security"))
                .forEach { linkerOpts("-framework", it) }
            linkerOpts("-lbz2", "-liconv", "-lexpat", "-lresolv", "-lxml2", "-lz", "-lc++")
            linkTaskProvider.configure {
                dependsOn(prepareMacosMpvRuntime)
                inputs.files(mpvFrameworks.map {
                    layout.buildDirectory.file("mpvkit/macos/$it.framework/$it")
                })
            }
        }
    }
}
