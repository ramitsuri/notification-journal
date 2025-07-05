import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

plugins {
    id("application")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.shadowJar)
}
group = "com.ramitsuri.notificationjournal"
version = libs.versions.appVersion.get()

application {
    mainClass = "com.ramitsuri.notificationjournal.server.MainKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation(libs.ktor.server)
    implementation(libs.ktor.ws)
    implementation(libs.logback)
    implementation(libs.logback.core)

    configurations.all {
        exclude(group = "org.slf4j", module = "slf4j-simple")
        exclude(group = "org.slf4j", module = "slf4j-nop")
    }

}
tasks.shadowJar {
    mergeServiceFiles()
    transform(ServiceFileTransformer::class.java) {
        setPath("META-INF/services")
    }
}
