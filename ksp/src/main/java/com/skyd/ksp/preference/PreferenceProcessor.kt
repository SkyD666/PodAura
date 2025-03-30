package com.skyd.ksp.preference

import androidx.datastore.preferences.core.Preferences
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName

class PreferenceProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    data class Entity(
        val preferenceQualifiedName: KSName,
        val preferenceSimpleName: KSName,
        val dataType: KSName,
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotationName = Preference::class.qualifiedName!!
        val symbols = resolver.getSymbolsWithAnnotation(annotationName)
        val entries = mutableListOf<Entity>()
        var basePreference: KSName? = null

        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            if (classDeclaration.classKind != ClassKind.OBJECT) {
                logger.error(
                    "The $annotationName annotation only applies to the object class.",
                    classDeclaration,
                )
                return@forEach
            }

            val keyProperty = classDeclaration.getAllProperties().firstOrNull {
                it.simpleName.asString() == "key"
            }
            if (keyProperty == null) {
                logger.error("Property named 'key' not found.", classDeclaration)
                return@forEach
            }
            if (basePreference == null) {
                basePreference = keyProperty.findOverridee()!!.parentDeclaration!!.qualifiedName!!
            }
            val keyPropertyReturnType = keyProperty.getter!!.returnType!!.resolve()
            if (keyPropertyReturnType.declaration.qualifiedName?.asString() !=
                Preferences.Key::class.qualifiedName
            ) {
                logger.error(
                    "The getter's return type of 'key' is not " +
                            "${Preferences.Key::class.qualifiedName}.", classDeclaration
                )
                return@forEach
            }

            val defaultProperty = classDeclaration.getAllProperties().firstOrNull {
                it.simpleName.asString() == "default"
            }
            if (defaultProperty == null) {
                logger.error("Property named 'default' not found.", classDeclaration)
                return@forEach
            }

            val defaultPropertyReturnType = defaultProperty.getter!!.returnType!!.resolve()
            val defaultPropertyReturnTypeQualifiedName =
                defaultPropertyReturnType.declaration.qualifiedName!!
            val keyPropertyGenericTypeQualifiedName =
                keyPropertyReturnType.arguments[0].type!!.resolve().declaration.qualifiedName!!

            if (defaultPropertyReturnTypeQualifiedName.asString() !=
                keyPropertyGenericTypeQualifiedName.asString()
            ) {
                logger.error(
                    "The type of 'default' is different from the generic type of 'key': " +
                            "default: ${defaultPropertyReturnTypeQualifiedName.asString()} " +
                            "key: ${keyPropertyGenericTypeQualifiedName.asString()}",
                    classDeclaration
                )
                return@forEach
            }

            entries += Entity(
                preferenceQualifiedName = classDeclaration.qualifiedName!!,
                preferenceSimpleName = classDeclaration.simpleName,
                dataType = defaultPropertyReturnType.declaration.qualifiedName!!,
            )
        }

        if (entries.isNotEmpty()) {
            generatePreferencesFile(entries, basePreference!!)
            generateCompositionLocalFile(entries)
            generateSettingsProviderFile(entries)
        }
        return emptyList()
    }

    private val packageName = "com.skyd.generated.preference"
    private fun generatePreferencesFile(entries: List<Entity>, basePreference: KSName) {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = packageName,
            fileName = "Preferences",
            extensionName = "kt",
        )
        file.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine("")
            writer.appendLine("import androidx.datastore.preferences.core.Preferences")
            writer.appendLine("import kotlin.reflect.KClass")
            writer.appendLine("")
            writer.appendLine("val preferences: List<Pair<${basePreference.asString()}<*>, KClass<*>>> = listOf(")
            for (entity in entries) {
                writer.append("    ")
                writer.appendLine("${entity.preferenceQualifiedName.asString()} to ${entity.dataType.asString()}::class,")
            }
            writer.appendLine(")")
        }
    }

    private fun generateCompositionLocalFile(entries: List<Entity>) {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = packageName,
            fileName = "PreferenceCompositionLocals",
            extensionName = "kt",
        )
        file.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine("")
            writer.appendLine("import androidx.datastore.preferences.core.Preferences")
            writer.appendLine("import androidx.compose.runtime.compositionLocalOf")
            writer.appendLine("")
            for (entity in entries) {
                val preferenceQualifiedName = entity.preferenceQualifiedName
                val localName = getLocalName(entity.preferenceSimpleName.asString())
                writer.appendLine("val $localName = compositionLocalOf { ${preferenceQualifiedName.asString()}.default }")
            }
        }
    }

    private fun generateSettingsProviderFile(entries: List<Entity>) {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = packageName,
            fileName = "SettingsProvider",
            extensionName = "kt",
        )
        file.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine("")
            writer.appendLine("import androidx.compose.runtime.Composable")
            writer.appendLine("import androidx.compose.runtime.CompositionLocalProvider")
            writer.appendLine("import androidx.compose.runtime.collectAsState")
            writer.appendLine("import androidx.compose.runtime.getValue")
            writer.appendLine("import androidx.compose.runtime.remember")
            writer.appendLine("import androidx.compose.ui.platform.LocalContext")
            writer.appendLine("import androidx.datastore.core.DataStore")
            writer.appendLine("import androidx.datastore.preferences.core.Preferences")
            writer.appendLine("import kotlinx.coroutines.Dispatchers")
            writer.appendLine("")
            writer.appendLine("@Composable")
            writer.appendLine("fun SettingsProvider(")
            writer.appendLine("    dataStore: DataStore<Preferences>,")
            writer.appendLine("    content: @Composable () -> Unit,")
            writer.appendLine(") {")
            writer.appendLine("    val context = LocalContext.current")
            writer.appendLine("    val pref by remember { dataStore.data }.collectAsState(initial = null, context = Dispatchers.Default)")
            writer.appendLine("    CompositionLocalProvider(")
            for (entity in entries) {
                val preferenceQualifiedName = entity.preferenceQualifiedName.asString()
                val localName = getLocalName(entity.preferenceSimpleName.asString())
                val keyString = "${preferenceQualifiedName}.key"
                val defaultString = "${preferenceQualifiedName}.default"
                writer.append("        ")
                writer.appendLine("$localName provides (pref?.get($keyString) ?: $defaultString),")
            }
            writer.appendLine("    ) { content() }")
            writer.appendLine("}")
        }
    }

    private fun getLocalName(preferenceSimpleName: String) = "Local" + preferenceSimpleName
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        .removeSuffix("Preference")
}