package com.skyd.podaura.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

abstract class VisualStudioVcVarsValueSource :
    ValueSource<File, VisualStudioVcVarsValueSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val configuredVcVarsPath: Property<String>
        val configuredInstallDirectory: Property<String>
        val vcInstallDirectory: Property<String>
        val visualStudioInstallDirectory: Property<String>
        val configuredVsWherePath: Property<String>
        val vsWhereEnvironmentPath: Property<String>
        val programFilesX86Directory: Property<String>
        val executableSearchPath: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): File {
        parameters.configuredVcVarsPath.orNull?.let(::File)?.let { configured ->
            checkFile(
                file = configured,
                message = "visualStudioVcVarsPath does not point to a file",
            )
            return configured.absoluteFile
        }
        parameters.configuredInstallDirectory.orNull?.let(::File)?.let { installDirectory ->
            val configured = installDirectory.resolve(VC_VARS_RELATIVE_PATH)
            checkFile(
                file = configured,
                message = "visualStudioInstallDir does not contain $VC_VARS_RELATIVE_PATH",
            )
            return configured.absoluteFile
        }

        sequenceOf(
            parameters.vcInstallDirectory.orNull
                ?.let(::File)
                ?.resolve("Auxiliary/Build/vcvars64.bat"),
            parameters.visualStudioInstallDirectory.orNull
                ?.let(::File)
                ?.resolve(VC_VARS_RELATIVE_PATH),
        ).filterNotNull().firstOrNull(File::isFile)?.let { return it.absoluteFile }

        findVsWhere()?.let { vsWhere ->
            val standardOutput = ByteArrayOutputStream()
            val result = execOperations.exec {
                commandLine(
                    vsWhere.absolutePath,
                    "-latest",
                    "-products", "*",
                    "-requires", "Microsoft.VisualStudio.Component.VC.Tools.x86.x64",
                    "-property", "installationPath",
                )
                isIgnoreExitValue = true
                this.standardOutput = standardOutput
                errorOutput = ByteArrayOutputStream()
            }
            if (result.exitValue == 0) {
                standardOutput.toString(Charsets.UTF_8)
                    .lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .map(::File)
                    .map { it.resolve(VC_VARS_RELATIVE_PATH) }
                    .firstOrNull(File::isFile)
                    ?.let { return it.absoluteFile }
            }
        }

        throw GradleException(
            "MSVC x64 tools were not found. Install the Visual Studio C++ workload, run Gradle " +
                    "from a Visual Studio Developer Command Prompt, or set " +
                    "-PvisualStudioVcVarsPath=<path>.",
        )
    }

    private fun findVsWhere(): File? {
        val pathCandidates = parameters.executableSearchPath.orNull
            .orEmpty()
            .split(File.pathSeparatorChar)
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { File(it.trim('"')).resolve("vswhere.exe") }
        return sequenceOf(
            parameters.configuredVsWherePath.orNull?.let(::File),
            parameters.vsWhereEnvironmentPath.orNull?.let(::File),
            parameters.programFilesX86Directory.orNull
                ?.let(::File)
                ?.resolve("Microsoft Visual Studio/Installer/vswhere.exe"),
        ).plus(pathCandidates).firstOrNull { it?.isFile == true }
    }

    private fun checkFile(file: File, message: String) {
        if (!file.isFile) {
            throw GradleException("$message: ${file.absolutePath}")
        }
    }

    private companion object {
        const val VC_VARS_RELATIVE_PATH = "VC/Auxiliary/Build/vcvars64.bat"
    }
}
