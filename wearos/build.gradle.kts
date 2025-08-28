plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.ramitsuri.notificationjournal"
    compileSdk = 35

    val appVersion = libs.versions.appVersion.get()
    defaultConfig {
        applicationId = "com.ramitsuri.notificationjournal"
        minSdk = 30
        targetSdk = 34
        versionCode = appVersion.toDouble().times(100).plus(1).toInt()
        versionName = appVersion

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Enable if testing
            // signingConfig signingConfigs.debug
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.ktx)
    implementation(libs.playservices.wearable)

    implementation(libs.androidx.activity.compose)
    val composeBom = platform(libs.composeBom)
    implementation(composeBom)
    implementation(libs.compose.runtime)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.toolingPreview)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.input)

    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout.material3)

    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.tiles)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.kotlin.datetime)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude { element -> element.file.toString().contains("generated/") }
        exclude { element -> element.file.toString().contains("build/") }
    }
}
