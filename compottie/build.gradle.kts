@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

// For skikoMain accessor
subprojects {
    plugins.apply("org.jetbrains.kotlin.multiplatform")
    plugins.apply("com.android.kotlin.multiplatform.library")

    extensions.configure<KotlinMultiplatformExtension> {
        explicitApi()
        applyDefaultHierarchyTemplate {
            common {
                group("skiko") {
                    withJvm()
                    withIos()
                    withMacos()
                }
            }
        }

        // Copied from generated accessors to avoid using them directly, as they are not available in the root project
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "io.github.alexzhirkevich.compottie.${project.name}"
            minSdk = 24
            compileSdk {
                version = release(37) { minorApiLevel = 0 }
            }
            buildToolsVersion = "37.0.0"
        }

        listOf(
            iosArm64(),
            iosSimulatorArm64(),
            macosArm64()
        ).forEach { target ->
            target.binaries.framework {
                baseName = "Compottie" + project.name.uppercaseFirstChar()
                isStatic = true
            }
        }

        jvm()

        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xexpect-actual-classes"
            )
            optIn.addAll(
                "androidx.compose.ui.InternalComposeUiApi",
                "kotlinx.serialization.ExperimentalSerializationApi",
                "kotlinx.coroutines.FlowPreview",
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.io.encoding.ExperimentalEncodingApi"
            )
        }
    }
}
