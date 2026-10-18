import com.skyd.podaura.buildlogic.MacOSJavaLauncher
import com.skyd.podaura.buildlogic.addMacOSAppLocalizations
import com.skyd.podaura.buildlogic.macOSAppLocalizations
import com.skyd.podaura.buildlogic.prepareMacOSJvmLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.compose.desktop.application.tasks.AbstractNativeMacApplicationPackageAppDirTask
import java.nio.file.Files

// Compose does not propagate nativeApplication.macOS.bundleID to the generated task.
tasks.withType<AbstractNativeMacApplicationPackageAppDirTask>().configureEach {
    bundleID = "com.skyd.podaura"
    inputs.property("appLocalizations", macOSAppLocalizations())
    doLast {
        addMacOSAppLocalizations(
            destinationDir.file("${packageName.get()}.app/Contents/Info.plist").get().asFile
        )
    }
}

if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
    pluginManager.withPlugin("org.jetbrains.compose") {
        // Compose registers and configures application tasks in afterEvaluate.
        afterEvaluate {
            val developmentApp = layout.buildDirectory.dir("macosJvm/PodAura.app")
            val standardLauncher = extensions.getByType<JavaToolchainService>()
                .launcherFor(extensions.getByType<JavaPluginExtension>().toolchain)
            val prepareJvmLauncher = tasks.register<Exec>("prepareMacosJvmLauncher") {
                val javaHome = standardLauncher.get().metadata.installationPath.asFile
                val app = developmentApp.get().asFile
                val libraries = app.resolve("Contents/lib").toPath()
                inputs.file(javaHome.resolve("bin/java"))
                inputs.property("javaHome", javaHome.absolutePath)
                inputs.property("localizations", macOSAppLocalizations())
                outputs.files(
                    app.resolve("Contents/MacOS/java"), app.resolve("Contents/Info.plist"),
                    app.resolve("Contents/_CodeSignature/CodeResources"),
                )
                outputs.upToDateWhen {
                    Files.isSymbolicLink(libraries) &&
                            Files.readSymbolicLink(libraries) == javaHome.resolve("lib").toPath()
                }
                doFirst { prepareMacOSJvmLauncher(javaHome, app) }
                commandLine("/usr/bin/codesign", "--force", "--sign", "-", app)
            }
            tasks.withType<JavaExec>().matching { it.name in setOf("run", "runRelease", "jvmRun") }
                .configureEach {
                    dependsOn(prepareJvmLauncher)
                    javaLauncher.set(standardLauncher.map {
                        MacOSJavaLauncher(it, developmentApp.get().file("Contents/MacOS/java"))
                    })
                    executable(developmentApp.get().file("Contents/MacOS/java").asFile.absolutePath)
                }

            tasks.named<AbstractExecTask<*>>("runDebugExecutableMacosArm64") {
                dependsOn("createDistributableNativeDebugMacosArm64")
                executable = layout.buildDirectory.file(
                    "compose/binaries/main/native-macosArm64-debug-app-image/PodAura.app/Contents/MacOS/PodAura"
                ).get().asFile.absolutePath
            }
            tasks.register<Exec>("runDistributableNativeDebugMacosArm64") {
                group = "compose desktop (native)"
                description = "Runs the Native app bundle with localized macOS system dialogs."
                val packager = tasks.named<AbstractNativeMacApplicationPackageAppDirTask>(
                    "createDistributableNativeDebugMacosArm64"
                ).get()
                dependsOn(packager)
                val name = packager.packageName.get()
                commandLine(
                    packager.destinationDir.file("$name.app/Contents/MacOS/$name").get().asFile
                )
            }
        }
    }
}
