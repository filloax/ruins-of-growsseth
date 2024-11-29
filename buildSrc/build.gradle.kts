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

    implementation(libs.plugin.kotlin.jvm)
    implementation(libs.plugin.kotlin.serialization)
    implementation(libs.plugin.dokka)

}