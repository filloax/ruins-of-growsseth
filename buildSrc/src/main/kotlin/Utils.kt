package com.ruslan.gradle

import gradle.kotlin.dsl.accessors._a3cefda71dba795f6746bc36999f0190.ext
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

fun Project.getFilloaxlib(loader: String = "common"): String {
    val useLocalJarFilloaxLib = (property("useLocalJarFilloaxLib") as String).toBoolean()
    val alwaysUseLocalMavenFilloaxLib = (property("alwaysUseLocalMavenFilloaxLib")!! as String).toBoolean()

    val libs = versionCatalogs.find("libs").get()
    val filloaxlibVersion = libs.findVersion("filloaxlib").get().toString()
    val useLocalMavenFilloaxLib = alwaysUseLocalMavenFilloaxLib || filloaxlibVersion.contains(Regex("rev\\d+"))

    return if (useLocalJarFilloaxLib)
        ":filloaxlib-${filloaxlibVersion}-${loader}"
    else if (useLocalMavenFilloaxLib)
        "com.filloax.filloaxlib:filloaxlib-${loader}:${filloaxlibVersion}"
    else if (loader == "common")
        "com.github.filloax.filloaxlib:filloaxlib-${loader}:${filloaxlibVersion}"
    else
        "maven.modrinth:filloaxlib:${filloaxlibVersion}-${loader}"
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

fun Project.addExtraResourceProp(key: String, value: String) {
    val extraProps = if (this.ext.has("extraProps")) {
        this.ext["extraProps"] as MutableMap<String, String>
    } else {
        mutableMapOf<String, String>().also { this.ext["extraProps"] = it }
    }
    extraProps[key] = value
}

val Project.extraResourceProps: Map<String, String> get() {
    return (this.ext["extraProps"] as MutableMap<String, String>?) ?: mapOf()
}