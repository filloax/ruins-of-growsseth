package com.ruslan.gradle

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
