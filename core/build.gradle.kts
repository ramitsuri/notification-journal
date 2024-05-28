plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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

            implementation(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ktx)
            implementation(libs.playservices.wearable)
            implementation(libs.playservices.coroutines)
        }
        jvmMain.dependencies {

        }
        jvmTest.dependencies {
            implementation(libs.androidx.room.testing)
        }
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
