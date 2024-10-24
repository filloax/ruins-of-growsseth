package com.ruslan.gradle

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.formats.DokkaJavadocPlugin

plugins {
    id("com.ruslan.gradle.multiloader-convention")
}

val modid: String by project

configurations {
    create(COMMON_JAVA) {
        isCanBeResolved = true
    }
    create(COMMON_RESOURCES) {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(project(BASE_PROJECT)) {
        capabilities {
            requireCapability("$group:$modid")
        }
    }
    COMMON_JAVA(project(path = BASE_PROJECT, configuration = COMMON_JAVA))
    COMMON_RESOURCES(project(path = BASE_PROJECT, configuration = COMMON_RESOURCES))
}

val preCompileTasks = PRE_COMPILE_TASKS.mapNotNull { try { project(BASE_PROJECT).tasks.getByName(it) } catch (e: Exception) {
        println("WARNING: ${e.message}")
        null
    } }

tasks.compileJava {
    preCompileTasks.forEach { dependsOn(it) }
    dependsOn(configurations.getByName(COMMON_JAVA))
    source(configurations.getByName(COMMON_JAVA))
}

tasks.compileKotlin {
    preCompileTasks.forEach { dependsOn(it) }
    dependsOn(configurations.getByName(COMMON_JAVA))
    source(configurations.getByName(COMMON_JAVA))
}

tasks.processResources {
    dependsOn(configurations.getByName(COMMON_RESOURCES))
    from(configurations.getByName(COMMON_RESOURCES))
}

tasks.named<Jar>("sourcesJar") {
    preCompileTasks.forEach { dependsOn(it) }

    dependsOn(configurations.getByName(COMMON_JAVA))
    from(configurations.getByName(COMMON_JAVA))
    dependsOn(configurations.getByName(COMMON_RESOURCES))
    from(configurations.getByName(COMMON_RESOURCES))
}

tasks.kotlinSourcesJar {
    preCompileTasks.forEach { dependsOn(it) }

    dependsOn(configurations.getByName(COMMON_JAVA))
    from(configurations.getByName(COMMON_JAVA))
    dependsOn(configurations.getByName(COMMON_RESOURCES))
    from(configurations.getByName(COMMON_RESOURCES))
}



// configure dokka to use our tasks
tasks.withType<DokkaTask>().configureEach {
    dependsOn(configurations.getByName(COMMON_RESOURCES))
    dokkaSourceSets {
        named("main") {
            sourceRoots.from(configurations.getByName(COMMON_JAVA))
        }
    }
}