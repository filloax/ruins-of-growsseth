package com.ruslan.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.io.File


fun File.copyRecursivelyWithFilter(target: File, filter: (String) -> Boolean): Boolean {
    var foundAny = false
    walkTopDown().forEach { file ->
        if (file.isFile) {
//            println("\tCopying with filter: $file ${file.readLines(Charsets.UTF_8).any(filter)}")
            val relativePath = toPath().relativize(file.toPath())
            val destFile = target.toPath().resolve(relativePath).toFile()
            if (file.readLines(Charsets.UTF_8).any {s -> filter(s)} ) {
                foundAny = true
                destFile.parentFile.mkdirs()
                file.copyTo(destFile, overwrite = true)
//                println("\tCopied $file to $destFile")
            }
        }
    }
    return foundAny
}

fun File.copyRecursivelyWithTransform(target: File, transform: (String) -> String) {
    walkTopDown().forEach { file ->
//        println("\tCopying with transform: $file")
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