import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

// Application binaries link the native media libraries already prepared by shared.
val artifacts =
    JsonSlurper().parse(rootProject.file("platform/apple/mpvkit/artifacts.json")) as Map<*, *>
val frameworks = (artifacts["artifacts"] as List<*>).map { (it as Map<*, *>)["name"].toString() }
val systemFrameworks = listOf(
    "AVFoundation", "AVKit", "AudioToolbox", "CoreAudio", "CoreVideo", "CoreMedia",
    "Metal", "IOSurface", "VideoToolbox", "QuartzCore",
)
pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        targets.withType<KotlinNativeTarget>().configureEach {
            val macos = konanTarget.family == Family.OSX
            if (!macos && konanTarget.family != Family.IOS) return@configureEach
            val sdk =
                if (macos) "macos" else if (konanTarget.name == "ios_arm64") "iphoneos" else "iphonesimulator"
            val nativeDirectory = project(":shared").layout.buildDirectory.dir("mpvkit/$sdk")
            binaries.configureEach {
                linkerOpts("-F${nativeDirectory.get().asFile}")
                val extra = if (macos) listOf(
                    "AppKit",
                    "OpenGL",
                    "IOKit",
                    "CoreFoundation",
                    "Security"
                ) else emptyList()
                (frameworks + systemFrameworks + extra).forEach { linkerOpts("-framework", it) }
                linkerOpts("-lbz2", "-liconv", "-lexpat", "-lresolv", "-lxml2", "-lz", "-lc++")
                linkTaskProvider.configure {
                    dependsOn(if (macos) ":shared:prepareMacosMpvRuntime" else ":shared:prepareIosMpvRuntime")
                    inputs.files(frameworks.map { nativeDirectory.map { dir -> dir.file("$it.framework/$it") } })
                }
            }
        }
    }
}
