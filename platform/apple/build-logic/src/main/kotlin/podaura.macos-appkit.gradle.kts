import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

// AppKitView uses the same internal interop nodes as UIKitView.
// The plugin is applied before the module declares its macOS target.
tasks.withType<KotlinNativeCompile>().matching {
    it.name == "compileKotlinMacosArm64"
}.configureEach {
    val composeUiMacosKlib =
        configurations.named("macosArm64CompileKlibraries").flatMap { configuration ->
            configuration.incoming.artifactView {
                componentFilter { id ->
                    id is ModuleComponentIdentifier &&
                            id.group == "org.jetbrains.compose.ui" && id.module == "ui-macosarm64"
                }
            }.files.elements.map { files -> files.single { it.asFile.extension == "klib" }.asFile }
        }
    inputs.file(composeUiMacosKlib).withPathSensitivity(PathSensitivity.NONE)
    compilerOptions.freeCompilerArgs.addAll(composeUiMacosKlib.map {
        listOf("-friend-modules", it.absolutePath)
    })
}
