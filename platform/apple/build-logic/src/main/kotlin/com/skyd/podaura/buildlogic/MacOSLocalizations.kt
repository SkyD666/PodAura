package com.skyd.podaura.buildlogic

import org.gradle.api.file.RegularFile
import org.gradle.jvm.toolchain.JavaLauncher
import java.io.File
import java.nio.file.Files

class MacOSJavaLauncher(
    private val delegate: JavaLauncher,
    private val executable: RegularFile,
) : JavaLauncher {
    override fun getMetadata() = delegate.metadata
    override fun getExecutablePath() = executable
}

/** Compose resources are outside .lproj directories, so AppKit needs this declaration. */
fun macOSAppLocalizations(): String = """
    <key>CFBundleLocalizations</key>
    <array>
        <string>en</string>
        <string>zh-Hans</string>
        <string>zh-Hant</string>
        <string>ja</string>
        <string>tr</string>
        <string>eo</string>
    </array>
""".trimIndent()

/** The Compose Native packaging task has no extraKeysRawXml setting. */
fun addMacOSAppLocalizations(plist: File) {
    val content = plist.readText()
    val index = content.lastIndexOf("</dict>")
    check(index >= 0) { "Missing root dictionary in $plist" }
    plist.writeText(content.take(index) + macOSAppLocalizations() + "\n" + content.substring(index))
}

/** Keep JavaExec's classpath and debugger arguments while giving AppKit a real app bundle. */
fun prepareMacOSJvmLauncher(javaHome: File, app: File) {
    val contents = app.resolve("Contents")
    val executable = contents.resolve("MacOS/java")
    executable.parentFile.mkdirs()
    javaHome.resolve("bin/java").copyTo(executable, overwrite = true)
    check(executable.setExecutable(true))
    val libraries = contents.resolve("lib").toPath()
    val target = javaHome.resolve("lib").toPath()
    if (Files.isSymbolicLink(libraries) && Files.readSymbolicLink(libraries) != target) {
        Files.delete(libraries)
    }
    if (!Files.isSymbolicLink(libraries)) Files.createSymbolicLink(libraries, target)
    contents.resolve("Info.plist").writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <plist version="1.0"><dict>
            <key>CFBundleIdentifier</key><string>com.skyd.podaura</string>
            <key>CFBundleName</key><string>PodAura</string>
            <key>CFBundleExecutable</key><string>java</string>
            <key>CFBundlePackageType</key><string>APPL</string>
            <key>CFBundleDevelopmentRegion</key><string>en</string>
            <key>NSHighResolutionCapable</key><true/>
            ${macOSAppLocalizations()}
        </dict></plist>
    """.trimIndent().trimStart()
    )
}
