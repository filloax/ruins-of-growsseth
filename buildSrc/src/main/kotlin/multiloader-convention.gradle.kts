package com.ruslan.gradle

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    idea

    kotlin("jvm")
}

val javaVersion: Int = (property("javaVersion")!! as String).toInt()
val javaVersionEnum = JavaVersion.values().find { it.majorVersion == javaVersion.toString() } ?: throw Exception("Cannot find java version for $javaVersion")

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)

    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = javaVersionEnum
    targetCompatibility = javaVersionEnum
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
    flatDir {
        dirs("libs")
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }

    exclusiveContent {
        forRepositories(
            maven {
                name = "ParchmentMC"
                url = uri("https://maven.parchmentmc.org/")
            },
            maven {
                name = "NeoForge"
                url = uri("https://maven.neoforged.net/releases")
            }
        )
        filter { includeGroup("org.parchmentmc.data") }
    }

    exclusiveContent {
        forRepositories(
            maven {
                name = "Team Resourceful Maven"
                url = uri("https://maven.teamresourceful.com/repository/maven-public/")
            }
        )
        filter { includeGroup("com.teamresourceful.resourcefulconfig") }
    }

    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }
}


val libs = project.versionCatalogs.find("libs")

// Project settings
val modid: String by project
val modName: String by project
val modDescription: String by project
val mavenGroup: String by project
val baseName: String by project
val author: String by project
val license: String by project
val displayUrl: String by project

val cydoVersion = (property("cydoVersion") as String).toBoolean()
val useLocalJarFxLib = (property("useLocalJarFxLib") as String).toBoolean()
val alwaysUseLocalMavenFXLib = (property("alwaysUseLocalMavenFXLib")!! as String).toBoolean()
val includeDeps = (property("includeDeps") as String).toBoolean()

// Main versions
val modVersion = libs.get().findVersion("modversion").get()
val kotlinVersion = libs.get().findVersion("kotlin").get()
val minecraftVersion = libs.get().findVersion("minecraft").get()
val minecraftVersionRange = libs.get().findVersion("minecraft.range").get()
val fapiVersion = libs.get().findVersion("fabric.api").get()
val fabricVersion = libs.get().findVersion("fabric").get()
val fabricKotlinVersion = libs.get().findVersion("fabric.language.kotlin").get()

// Libraries
val fxlibVersion = libs.get().findVersion("fxlib").get().toString()
val rconfigVersion = libs.get().findVersion("rconfig").get().toString()
val rconfigMcVersion = libs.get().findVersion("rconfigMc").get().toString()

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${modName}" }
    }

    manifest {
        attributes(mapOf(
                "Specification-Title"     to modName,
                "Specification-Vendor"    to author,
                "Specification-Version"   to modVersion,
                "Implementation-Title"    to modName,
                "Implementation-Version"  to modVersion,
                "Implementation-Vendor"   to author,
                "Built-On-Minecraft"      to minecraftVersion
        ))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.valueOf("JVM_$javaVersion"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    this.options.encoding = "UTF-8"
    this.options.release.set(javaVersion)
    options.compilerArgs.addAll(listOf("-Xlint:all,-classfile,-processing,-deprecation,-serial", "-Xdoclint:none"))
//    options.compilerArgs.addAll(listOf("-Werror")) // cannot use werror as false positives obtained from mixin (maybe)
}

tasks.withType<ProcessResources>().configureEach {
    exclude(".cache")

    val expandProps = mapOf(
        "version_prefix" to "$modVersion-$minecraftVersion",
        "group" to project.group, // Else we target the task's group.
        "display_url" to displayUrl, // Else we target the task's group.
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "fabric_kotlin_version" to fabricKotlinVersion,
        "fabric_loader_version" to fabricVersion,
        "fapi_version" to fapiVersion,

        "fxlib_version" to fxlibVersion,
        "rconfig_version" to rconfigVersion,

        "mod_name" to modName,
        "author" to author,
        "mod_id" to modid,
        "license" to license,
        "description" to modDescription,

        // non-meta, functional config
        "cydoniaMode" to cydoVersion,
    )

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "META-INF/mods.toml", "cydonia.properties")) {
        expand(expandProps)
    }

    inputs.properties(expandProps)
}

publishing {
    repositories {
        mavenLocal()
    }
}
