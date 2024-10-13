package com.ruslan.gradle

import gradle.kotlin.dsl.accessors._a3cefda71dba795f6746bc36999f0190.versionCatalogs
import org.gradle.api.Project
import java.io.File


fun File.copyRecursivelyWithFilter(target: File, filter: (String) -> Boolean) {
    walkTopDown().forEach { file ->
        if (file.isFile) {
            val relativePath = toPath().relativize(file.toPath())
            val destFile = target.toPath().resolve(relativePath).toFile()
            if (file.readLines(Charsets.UTF_8).any(filter)) {
                destFile.parentFile.mkdirs()
                file.copyTo(destFile, overwrite = true)
//                println("Copied $file to $destFile")
            }
        }
    }
}

fun File.copyRecursivelyWithTransform(target: File, transform: (String) -> String) {
    walkTopDown().forEach { file ->
        if (file.isFile) {
            val relativePath = toPath().relativize(file.toPath())
            val destFile = target.toPath().resolve(relativePath).toFile()
            destFile.parentFile.mkdirs()
            destFile.writer(Charsets.UTF_8).use { writer ->
                file.readLines(Charsets.UTF_8).forEach { line ->
                    writer.appendLine(transform(line))
                }
            }
//            println("Copied $file to $destFile")
        }
    }
}

fun Project.getFxlib(loader: String = "common"): String {
    val useLocalJarFxLib = (property("useLocalJarFxLib") as String).toBoolean()
    val alwaysUseLocalMavenFXLib = (property("alwaysUseLocalMavenFXLib")!! as String).toBoolean()

    val libs = versionCatalogs.find("libs").get()
    val fxlibVersion = libs.findVersion("fxlib").get().toString()
    val useLocalMavenFxLib = alwaysUseLocalMavenFXLib || fxlibVersion.contains(Regex("rev\\d+"))

    return (if (useLocalJarFxLib)
        ":fx-lib-${fxlibVersion}-${loader}"
    else if (useLocalMavenFxLib)
        "com.filloax.fxlib:fx-lib-${loader}:${fxlibVersion}"
    else
        "com.github.filloax:fx-lib:v${fxlibVersion}-${loader}"
            )
}

// neoforge doesn't have mapping issues
fun Project.getResourcefulConfig(loader: String = "neoforge"): String {
    val libs = versionCatalogs.find("libs").get()
    val mcVersion = libs.findVersion("minecraft").get().toString()
    val rmcVersion = libs.findVersion("rconfigMc").get().toString()
    val version = libs.findVersion("rconfig").get().toString()

    return "com.teamresourceful.resourcefulconfig" +
            ":resourcefulconfig-${loader}-${if (rmcVersion == "") mcVersion else rmcVersion}" +
            ":$version"
}
