import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.buildKonfig)
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

            implementation(libs.diffUtils)
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
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.multiplatform.settings)

            implementation(libs.amqp.client)

            implementation(libs.datastore)
            implementation(libs.symspellkt)
            implementation(libs.symspellkt.fdic)

            implementation(libs.kermit)
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
            implementation (libs.jsystemthemedetector)
        }
        jvmTest.dependencies {
            implementation(libs.androidx.room.testing)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
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
    compileSdk = 35

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
        mainClass = "com.ramitsuri.notificationjournal.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Journal"
            packageVersion = "1.0.0"

            val iconsRoot = project.file("desktop-icons")
            macOS {
                iconFile.set(iconsRoot.resolve("macos.icns"))
            }
            // https://issuetracker.google.com/issues/342609814
            modules("java.instrument", "java.naming", "java.prefs", "java.security.sasl", "jdk.unsupported")
        }
    }
}

buildkonfig {
    packageName = "com.ramitsuri.notificationjournal.core"

    val isDebug = getLocalProperty("isDebug", "false") ?: "false"
    val appVersion = libs.versions.appVersion.get()

    defaultConfigs {
        buildConfigField(type = BOOLEAN, name = "IS_DEBUG", value = isDebug, const = true)
        buildConfigField(type = STRING, name = "APP_VERSION", value = appVersion, const = true)
    }
}

fun getLocalProperty(
    key: String,
    defaultValue: String? = null,
): String? {
    val file = "local.properties"
    return getProperty(file, key, defaultValue)
}

fun getProperty(
    fileName: String,
    key: String,
    defaultValue: String? = null,
): String? {
    return if (file(rootProject.file(fileName)).exists()) {
        val properties = Properties()
        properties.load(FileInputStream(file(rootProject.file(fileName))))
        properties.getProperty(key, defaultValue)
    } else {
        defaultValue
    }
}