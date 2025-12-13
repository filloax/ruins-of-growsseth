package com.ruslan.gradle

import org.jetbrains.dokka.gradle.DokkaTask

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

tasks.compileJava {
    dependsOn(configurations.getByName(COMMON_JAVA))
    source(configurations.getByName(COMMON_JAVA))
}

tasks.compileKotlin {
    dependsOn(configurations.getByName(COMMON_JAVA))
    source(configurations.getByName(COMMON_JAVA))
}

tasks.processResources {
    dependsOn(configurations.getByName(COMMON_RESOURCES))
    from(configurations.getByName(COMMON_RESOURCES))
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.getByName(COMMON_JAVA))
    from(configurations.getByName(COMMON_JAVA))
    dependsOn(configurations.getByName(COMMON_RESOURCES))
    from(configurations.getByName(COMMON_RESOURCES))
}

tasks.kotlinSourcesJar {
    dependsOn(configurations.getByName(COMMON_JAVA))
    from(configurations.getByName(COMMON_JAVA))
    dependsOn(configurations.getByName(COMMON_RESOURCES))
    from(configurations.getByName(COMMON_RESOURCES))
}



// configure dokka to use our tasks
dokka {
    dokkaSourceSets.main {
        sourceRoots.from(configurations.getByName(COMMON_JAVA))
    }
}
