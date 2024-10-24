package com.ruslan.gradle

import org.gradle.api.Project

class TokenReplaceDirs(project: Project) {
    val baseSourceRoot = "src/main"
    val backupRoot = "${project.layout.buildDirectory.get().asFile.absolutePath}/backup-src"
    val processedRoot = "${project.layout.buildDirectory.get().asFile.absolutePath}/processed-src"

    val affected = listOf("java", "kotlin")

    val mainSources = affected.map{project.file("$baseSourceRoot/$it")}
    val backupSources = affected.map{project.file("$backupRoot/$it")}
    val processedSources = affected.map{project.file("$processedRoot/$it")}
}