plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // For some reason not specifying GSon version and using recent ones currently (21/07/2024) breaks Loom
    // see https://github.com/orgs/FabricMC/discussions/3546 (fixes there other than this workaround didn't work)
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.0.0")
    implementation(libs.kotlin.jvm)
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0-Beta")
}