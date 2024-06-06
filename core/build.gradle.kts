import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    jvm()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.savedstate)
            implementation(libs.lifecycle.runtime.compose)

            implementation(libs.room.ktx)
            implementation(libs.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.kotlin.datetime)

            implementation(libs.kotlin.serialization)
            implementation(libs.ktor.core)
            implementation(libs.ktor.okhttp)
            implementation(libs.ktor.content.negotation)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.logging)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ktx)
            implementation(libs.playservices.wearable)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.playservices.coroutines)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        jvmTest.dependencies {
            implementation(libs.androidx.room.testing)
        }
    }

    // To fix `java.lang.IllegalStateException: Module with the Main dispatcher had failed to
    // initialize.` For some reason android artifact was being pulled, found this as the only way to
    // fix it.
    configurations.commonMainApi {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
    }
}

android {
    namespace = "com.ramitsuri.notificationjournal.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspJvmTest", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    publicResClass = true
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.ramitsuri.notificationjournal.desktop"
            packageVersion = "1.0.0"
        }
    }
}