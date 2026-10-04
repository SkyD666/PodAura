import groovy.json.JsonSlurper
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.konan.target.Family

// MPVKit download, C interop and native test linking. Targets/frameworks remain owned by the module.
val prepareIosMpv = tasks.register<Exec>("prepareIosMpv") {
    val script = rootProject.file("platform/ios/mpvkit/prepare.py")
    inputs.files(script, rootProject.file("platform/ios/mpvkit/artifacts.json"),
        rootProject.file("platform/ios/mpvkit/podaura.h"))
    // Gradle removes stale outputs on a fresh checkout; it must never own the native binaries here.
    outputs.dirs(layout.buildDirectory.dir("mpvkit/iphoneos/Libmpv.framework/Headers"),
        layout.buildDirectory.dir("mpvkit/iphonesimulator/Libmpv.framework/Headers"))
    commandLine("python3", script.absolutePath, "--headers-only")
}

val prepareIosMpvRuntime = tasks.register<Exec>("prepareIosMpvRuntime") {
    dependsOn(prepareIosMpv)
    commandLine("python3", rootProject.file("platform/ios/mpvkit/prepare.py").absolutePath)
}

val downloadIosMpvCertificates = tasks.register<Exec>("downloadIosMpvCertificates") {
    val script = rootProject.file("platform/ios/mpvkit/prepare.py")
    inputs.files(script, rootProject.file("platform/ios/mpvkit/artifacts.json"))
    outputs.file(layout.buildDirectory.file("mpvkit/downloads/cacert.pem"))
    commandLine("python3", script.absolutePath, "--cert-only")
}

val prepareIosMpvResources = tasks.register<Sync>("prepareIosMpvResources") {
    from("src/iosMain/composeResources")
    from(downloadIosMpvCertificates) { into("files") }
    into(layout.buildDirectory.dir("generated/iosMpvResources"))
}

extensions.configure<ComposeExtension> {
    extensions.configure<ResourcesExtension> {
        customDirectory("iosMain", layout.dir(prepareIosMpvResources.map { it.destinationDir }))
    }
}

val artifacts = JsonSlurper().parse(rootProject.file("platform/ios/mpvkit/artifacts.json")) as Map<*, *>
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
        tasks.named(interop.interopProcessingTaskName).configure { dependsOn(prepareIosMpv) }
        binaries.withType<TestExecutable>().configureEach {
            linkerOpts("-F${layout.buildDirectory.dir("mpvkit/$sdk").get().asFile}")
            (mpvFrameworks + systemFrameworks).forEach { linkerOpts("-framework", it) }
            linkerOpts("-lbz2", "-liconv", "-lexpat", "-lresolv", "-lxml2", "-lz", "-lc++")
            linkTaskProvider.configure { dependsOn(prepareIosMpvRuntime) }
        }
    }
}
