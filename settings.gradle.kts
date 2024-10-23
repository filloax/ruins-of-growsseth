pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Parchment"
            url = uri("https://maven.parchmentmc.org")
            content {
                includeGroupAndSubgroups("org.parchmentmc")
            }
        }
        maven {
            name = "Sponge"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
            content {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }
    }

    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion apply false
        kotlin("plugin.serialization") version kotlinVersion apply false
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

val modid: String by settings

rootProject.name = modid

listOf(
    "base",
    "neoforge",
    "fabric",
).forEach { include(it) }