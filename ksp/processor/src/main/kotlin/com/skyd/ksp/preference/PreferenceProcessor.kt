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
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.skyd.ksp.annotation.Preference
import com.skyd.ksp.annotation.PreferencesList

class PreferenceProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    data class Entity(
        val preferenceQualifiedName: KSName,
        val preferenceSimpleName: KSName,
        val dataType: KSName,
    )

    private fun KSPropertyDeclaration.findRootOverridee(): KSPropertyDeclaration? {
        var lastOverridee = findOverridee() ?: return null
        var overridee: KSPropertyDeclaration? = lastOverridee
        while (overridee != null) {
            lastOverridee = overridee
            overridee = overridee.findOverridee()
        }
        return lastOverridee
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("PreferenceProcessor start: ${resolver.getModuleName().asString()}")

        val preferencesListSymbols =
            resolver.getSymbolsWithAnnotation(PreferencesList::class.qualifiedName!!)
        val preferencesListSymbol = preferencesListSymbols
            .filterIsInstance<KSPropertyDeclaration>().firstOrNull() ?: return emptyList()
        val preferencesListPkg = preferencesListSymbol
            .qualifiedName!!.asString().substringBeforeLast(".")

        val annotationName = Preference::class.qualifiedName!!
        val symbols = resolver.getSymbolsWithAnnotation(annotationName)
            .filterIsInstance<KSClassDeclaration>().filter { declaration ->
                val excludeFromList = declaration.annotations.first {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName
                }.arguments.first {
                    it.name?.asString() == Preference::excludeFromList.name
                }
                !(excludeFromList.value as Boolean)
            }
        val entries = mutableListOf<Entity>()
        var basePreference: KSName? = null

        symbols.forEach { classDeclaration ->
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
            val baseKey: KSPropertyDeclaration = keyProperty.findRootOverridee()!!
            if (basePreference == null) {
                basePreference = baseKey.parentDeclaration!!.qualifiedName!!
            }
            val baseKeyPropertyReturnType = baseKey.getter!!.returnType!!.resolve()
            if (baseKeyPropertyReturnType.declaration.qualifiedName?.asString() !=
                Preferences.Key::class.qualifiedName
            ) {
                logger.error(
                    "The getter's return type of 'key' is not " +
                            "${Preferences.Key::class.qualifiedName}. " +
                            "It's ${baseKeyPropertyReturnType.declaration.qualifiedName}",
                    classDeclaration
                )
                return@forEach
            }

            val defaultProperty = classDeclaration.getAllProperties().firstOrNull {
                it.simpleName.asString() == "default"
            }
            if (defaultProperty?.getter?.returnType == null) {
                logger.error("Property named 'default' not found.", classDeclaration)
                return@forEach
            }
            val defaultPropertyReturnType =
                defaultProperty.getter?.returnType?.resolve() ?: return@forEach
            entries += Entity(
                preferenceQualifiedName = classDeclaration.qualifiedName ?: return@forEach,
                preferenceSimpleName = classDeclaration.simpleName,
                dataType = defaultPropertyReturnType.declaration.qualifiedName ?: return@forEach,
            )
        }
        if (entries.isNotEmpty()) {
            generatePreferencesFile(entries, basePreference!!, preferencesListPkg)
        }
        return emptyList()
    }

    private fun generatePreferencesFile(
        entries: List<Entity>,
        basePreference: KSName,
        pkg: String
    ) {
        // For reproducible builds, sort by name.
        val sortedEntities = entries.sortedBy { it.preferenceSimpleName.asString() }
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = pkg,
            fileName = "Preferences",
            extensionName = "kt",
        )
        file.bufferedWriter().use { writer ->
            writer.appendLine("package $pkg")
            writer.appendLine("")
            writer.appendLine("import androidx.datastore.preferences.core.Preferences")
            writer.appendLine("import kotlin.reflect.KClass")
            writer.appendLine("")
            writer.appendLine("actual val preferences: List<Pair<${basePreference.asString()}<*>, KClass<*>>> = listOf(")
            for (entity in sortedEntities) {
                writer.append("    ")
                writer.appendLine("${entity.preferenceQualifiedName.asString()} to ${entity.dataType.asString()}::class,")
            }
            writer.appendLine(")")
        }
    }
}
