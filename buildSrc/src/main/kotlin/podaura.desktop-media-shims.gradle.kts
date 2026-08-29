import com.skyd.podaura.buildlogic.VisualStudioVcVarsValueSource

// Native desktop media bridges and their JVM/application resource wiring.

val buildJvmArch = System.getProperty("os.arch").lowercase()
val buildOperatingSystem = System.getProperty("os.name").lowercase()
val desktopMediaShimAppResources = layout.buildDirectory.dir(
    "generated/desktopMediaPlayer/appResources"
)

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

val compileMacMediaPlayerShim = macMediaShimTarget?.let { (nativeArchitecture, _) ->
    tasks.register<Exec>("compileMacMediaPlayerShim") {
        group = "desktop media"
        description = "Builds the macOS native media session shim."
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
        group = "desktop media"
        description = "Copies the macOS media shim into the JVM classpath resources."
        dependsOn(compileTask)
        from(macMediaShimBinary)
        into(macMediaShimJvmResources.map { it.dir(resourcePrefix) })
    }
}

val prepareMacMediaPlayerAppResources = macMediaShimTarget?.let {
    val compileTask = requireNotNull(compileMacMediaPlayerShim)
    tasks.register<Sync>("prepareMacMediaPlayerAppResources") {
        group = "desktop media"
        description = "Copies the macOS media shim into the Compose application resources."
        dependsOn(compileTask)
        from(macMediaShimBinary)
        into(desktopMediaShimAppResources.map { it.dir("macos") })
    }
}

val windowsMediaShimTarget = buildOperatingSystem.startsWith("windows") &&
        buildJvmArch in setOf("amd64", "x86_64")
val windowsMediaShimSourceDirectory = rootProject.file(
    "fundation/src/jvmMain/cpp/windowsMediaPlayer"
)
val windowsMediaShimSources = fileTree(windowsMediaShimSourceDirectory) {
    include("*.cpp")
}
val windowsMediaShimHeaders = fileTree(windowsMediaShimSourceDirectory) {
    include("*.h")
}
val windowsMediaShimBinary = layout.buildDirectory.file(
    "generated/windowsMediaPlayer/native/podaura_windows_media_player.dll"
)
val windowsMediaShimJvmResources = layout.buildDirectory.dir(
    "generated/windowsMediaPlayer/jvmResources"
)

val compileWindowsMediaPlayerShim = if (windowsMediaShimTarget) {
    val vcVars = providers.of(VisualStudioVcVarsValueSource::class) {
        parameters.configuredVcVarsPath.set(
            providers.gradleProperty("visualStudioVcVarsPath")
        )
        parameters.configuredInstallDirectory.set(
            providers.gradleProperty("visualStudioInstallDir")
        )
        parameters.vcInstallDirectory.set(providers.environmentVariable("VCINSTALLDIR"))
        parameters.visualStudioInstallDirectory.set(
            providers.environmentVariable("VSINSTALLDIR")
        )
        parameters.configuredVsWherePath.set(providers.gradleProperty("vswherePath"))
        parameters.vsWhereEnvironmentPath.set(providers.environmentVariable("VSWHERE"))
        parameters.programFilesX86Directory.set(
            providers.environmentVariable("ProgramFiles(x86)")
        )
        parameters.executableSearchPath.set(providers.environmentVariable("PATH"))
    }
    val commandInterpreter = providers.environmentVariable("ComSpec")
        .orNull
        ?.let(::file)
        ?.takeIf(File::isFile)
        ?.absolutePath
        ?: "cmd.exe"
    val windowsMediaShimOutputFile = windowsMediaShimBinary.get().asFile
    val windowsMediaShimOutputDirectory = windowsMediaShimOutputFile.parentFile
    val windowsMediaShimCompileArguments = listOf(
        "cl.exe",
        "/nologo",
        "/LD",
        "/MD",
        "/EHsc",
        "/std:c++17",
        "/permissive-",
        "/W4",
        "/DUNICODE",
        "/D_UNICODE",
        "/DWINVER=0x0A00",
        "/D_WIN32_WINNT=0x0A00",
        "/I\"${windowsMediaShimSourceDirectory.absolutePath}\"",
        windowsMediaShimSources.files
            .sortedBy { it.name }
            .joinToString(" ") { source -> "\"${source.absolutePath}\"" },
        "/Fe:\"${windowsMediaShimOutputFile.absolutePath}\"",
        "/link",
        "windowsapp.lib",
        "runtimeobject.lib",
        "ole32.lib",
        "shell32.lib",
        "comctl32.lib",
        "advapi32.lib",
        "user32.lib",
        "gdi32.lib",
    )
    tasks.register<Exec>("compileWindowsMediaPlayerShim") {
        group = "desktop media"
        description = "Builds the Windows native media session and taskbar shim."
        inputs.files(windowsMediaShimHeaders, windowsMediaShimSources, vcVars)
        inputs.property("visualStudioVcVarsPath", vcVars.map { it.absolutePath })
        inputs.property("windowsCommandInterpreter", commandInterpreter)
        outputs.file(windowsMediaShimBinary)
        doFirst {
            check(
                windowsMediaShimOutputDirectory.mkdirs() ||
                        windowsMediaShimOutputDirectory.isDirectory
            )
            val windowsMediaShimCompileCommand = listOf(
                "call \"${vcVars.get().absolutePath}\" >nul",
                "&&",
            ).plus(windowsMediaShimCompileArguments).joinToString(" ")
            commandLine(
                commandInterpreter,
                "/d",
                "/c",
                windowsMediaShimCompileCommand,
            )
        }
        workingDir(windowsMediaShimOutputDirectory)
    }
} else {
    null
}

val prepareWindowsMediaPlayerJvmResources = compileWindowsMediaPlayerShim?.let { compileTask ->
    tasks.register<Sync>("prepareWindowsMediaPlayerJvmResources") {
        group = "desktop media"
        description = "Copies the Windows media shim into the JVM classpath resources."
        dependsOn(compileTask)
        from(windowsMediaShimBinary)
        into(windowsMediaShimJvmResources.map { it.dir("win32-x86-64") })
    }
}

val prepareWindowsMediaPlayerAppResources = compileWindowsMediaPlayerShim?.let { compileTask ->
    tasks.register<Sync>("prepareWindowsMediaPlayerAppResources") {
        group = "desktop media"
        description = "Copies the Windows media shim into the Compose application resources."
        dependsOn(compileTask)
        from(windowsMediaShimBinary)
        into(desktopMediaShimAppResources.map { it.dir("windows") })
    }
}

tasks.withType<ProcessResources>().configureEach {
    if (name == "jvmProcessResources") {
        prepareMacMediaPlayerJvmResources?.let { prepareResources ->
            dependsOn(prepareResources)
            from(macMediaShimJvmResources)
        }
        prepareWindowsMediaPlayerJvmResources?.let { prepareResources ->
            dependsOn(prepareResources)
            from(windowsMediaShimJvmResources)
        }
    }
}

listOfNotNull(
    prepareMacMediaPlayerAppResources,
    prepareWindowsMediaPlayerAppResources,
).forEach { prepareResources ->
    tasks.configureEach {
        if (name == "prepareAppResources") {
            dependsOn(prepareResources)
        }
    }
}
