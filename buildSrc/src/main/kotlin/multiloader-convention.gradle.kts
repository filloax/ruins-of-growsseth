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

base {
    archivesName = property("archives_base_name") as String
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    mavenLocal()
//    flatDir {
//        dirs("libs")
//    }

    exclusiveContent {
        forRepository { maven("https://jitpack.io") }
        filter { includeGroupByRegex("com\\.github\\.(stuhlmeier|filloax).*") }
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

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter { includeGroup("maven.modrinth") }
    }

    maven("https://api.modrinth.com/maven")
    maven("https://maven.terraformersmc.com/releases")
}

//region Libs and props
val libs = project.versionCatalogs.find("libs").get()

// Project settings
val modid: String by project
val modName: String by project
val modIcon: String by project
val mavenGroup: String by project
val baseName: String by project
val author: String by project
val license: String by project
val displayUrl: String by project

val cydoVersion = (property("cydoVersion") as String).toBoolean()

// Main versions
val modVersion = libs.findVersion("modversion").get()
val kotlinVersion = libs.findVersion("kotlin").get()
val minecraftVersion = libs.findVersion("minecraft").get()
val minecraftVersionRange = libs.findVersion("minecraft.range").get()
val fabricMinecraftVersionRange = libs.findVersion("minecraft.range.fabric").get()
val fapiVersion = libs.findVersion("fabric.api").get()
val fabricVersion = libs.findVersion("fabric").get()
val fabricKotlinVersion = libs.findVersion("fabric.language.kotlin").get()
val neoforgeVersion = libs.findVersion("neoforge").get()
val neoforgeVersionRange = libs.findVersion("neoforge.range").get()
val fmlVersionRange = libs.findVersion("fml.range").get()
val kotlinforgeVersion = libs.findVersion("kotlinforge").get()
val kotlinforgeVersionRange = libs.findVersion("kotlinforge.range").get()

// Libraries
val filloaxlibVersion = libs.findVersion("filloaxlib").get().toString()
val rconfigVersion = libs.findVersion("rconfig").get().toString()
val rconfigMcVersion = libs.findVersion("rconfigMc").get().toString()
//endregion

//region Artifacts
// Declare capabilities on the outgoing configurations.
// Read more about capabilities here: https://docs.gradle.org/current/userguide/component_capabilities.html#sec:declaring-additional-capabilities-for-a-local-component
listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements").forEach { variant ->
    configurations.getByName(variant).outgoing {
        capability("$group:${base.archivesName.get()}:$version")
        capability("$group:$modid-${project.name}-${minecraftVersion}:$version")
        capability("$group:$modid:$version")
    }
    publishing.publications.withType<MavenPublication>().configureEach {
        suppressPomMetadataWarningsFor(variant)
    }
}
//endregion

//region Task configuration

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${modName}" }
    }
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${modName}" }
    }

    // Cydo version: remove structure spawns
    if (cydoVersion) {
        println("Cydo version: will exclude structure spawns...")
        exclude("data/growsseth/worldgen/structure_set/**")
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

    val metaProps = mapOf(
        "version_prefix" to "$modVersion-$minecraftVersion",
        "group" to project.group, // Else we target the task's group.
        "display_url" to displayUrl, // Else we target the task's group.
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "fabric_minecraft_version_range" to fabricMinecraftVersionRange,
        "fabric_kotlin_version" to fabricKotlinVersion,
        "fabric_loader_version" to fabricVersion,
        "fapi_version" to fapiVersion,

        "neoforge_version" to neoforgeVersion,
        "neoforge_version_range" to neoforgeVersionRange,
        "fml_version_range" to fmlVersionRange,
        "kotlinforge_version" to kotlinforgeVersion,
        "kotlinforge_version_range" to kotlinforgeVersionRange,

        "filloaxlib_version" to filloaxlibVersion,
        "rconfig_version" to rconfigVersion,

        "mod_name" to modName,
        "author" to author,
        "mod_id" to modid,
        "mod_icon" to modIcon,
        "license" to license,
    )
    val cydoProps = mapOf(
        // non-meta, functional config
        "cydoniaMode" to cydoVersion,
    )

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "META-INF/neoforge.mods.toml")) {
        expand(metaProps + project.extraResourceProps)
    }
    filesMatching(listOf("cydonia.properties")) {
        expand(cydoProps)
    }

    inputs.properties(metaProps)
    inputs.properties(cydoProps)
}
//endregion

// Publishing
publishing {
    repositories {
        mavenLocal()
    }
}

// IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
