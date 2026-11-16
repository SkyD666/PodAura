plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.skyd.podnya"
        minSdk = 24
        compileSdk { version = release(37) { minorApiLevel = 2 } }
        buildToolsVersion = "37.0.0"
        androidResources.enable = true
        optimization.consumerKeepRules.file("src/androidMain/koog-consumer-rules.pro")
        withHostTest {}
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    // Keep macOS Native on the unavailable implementation; other targets share Koog.
    applyDefaultHierarchyTemplate()
    sourceSets {
        val koogMain = create("koogMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.koog.agents.core)
                implementation(libs.koog.openai.client)
                implementation(libs.koog.anthropic.client)
                implementation(libs.koog.google.client)
                implementation(libs.koog.http.client.ktor)
                implementation(libs.kotlin.logging)
            }
        }
        androidMain { dependsOn(koogMain) }
        jvmMain { dependsOn(koogMain) }
        iosMain { dependsOn(koogMain) }
        val koogTest = create("koogTest") {
            dependsOn(commonTest.get())
            dependencies { implementation(libs.koog.agents.test) }
        }
        jvmTest { dependsOn(koogTest) }
        iosTest { dependsOn(koogTest) }
        named("androidHostTest") { dependsOn(koogTest) }

        commonMain.dependencies {
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.materialIconsExtended)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigationevent)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.skyd666.compone)
            implementation(libs.skyd666.mvi)
            implementation(libs.ksoup)
            implementation(libs.markdown.m3)
            implementation(projects.fundation)
            implementation(projects.sharedApi)
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
        }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.compose.ui.test.junit4)
            implementation(libs.jetbrains.navigation3.ui)
        }
    }
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.compose.resources.ExperimentalResourceApi",
            "org.jetbrains.compose.resources.InternalResourceApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.uuid.ExperimentalUuidApi",
            "io.ktor.utils.io.InternalAPI",
        )
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.skyd.podnya.resources"
}

dependencies {
    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64", "kspMacosArm64").forEach {
        add(it, libs.androidx.room3.compiler)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
