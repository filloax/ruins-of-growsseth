package com.ruslan.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.*
import java.io.File

open class TransformTokensTask : DefaultTask() {
    init {
        group = "custom"
        description = "Backups and transforms sources by replacing tokens"

        setProjectVariables(TokenReplaceDirs(project))
    }

    @get:InputDirectory
    lateinit var mainSourceRoot: File

    private lateinit var mainSourceDirs: List<File>

    @get:OutputDirectory
    lateinit var backupRootDir: File
    @get:OutputDirectory
    lateinit var processedRootDir: File
    @get:OutputDirectories
    lateinit var backupSources: List<File>
    @get:OutputDirectories
    lateinit var processedSources: List<File>

    private val tokens = mutableMapOf<String, String>()

    @Suppress("unused") // Used in build script
    fun replaceTokens(tokens: Map<String, String>) {
        this.tokens += tokens
    }

    private fun setProjectVariables(tokenReplaceDirs: TokenReplaceDirs) {
        mainSourceDirs = tokenReplaceDirs.mainSources
        mainSourceRoot = project.file(tokenReplaceDirs.baseSourceRoot)

        backupRootDir = project.file(tokenReplaceDirs.backupRoot)
        processedRootDir = project.file(tokenReplaceDirs.processedRoot)
        backupSources = tokenReplaceDirs.backupSources
        processedSources = tokenReplaceDirs.processedSources
    }

    @TaskAction
    fun run() {
        processedRootDir.deleteRecursively()
        processedRootDir.mkdirs()
        backupRootDir.deleteRecursively()
        backupRootDir.mkdirs()

        if (tokens.isEmpty()) {
            throw InvalidUserDataException("Token replacement didn't have any tokens configured!")
        }

        var anySuccess = false
        mainSourceDirs.zip(backupSources).forEach { (main, backup) ->
            println("Backupping matching $main sources to $backup...")
            anySuccess = main.copyRecursivelyWithFilter(backup) { s -> tokens.keys.any { s.contains(it) } } || anySuccess
        }
        if (!anySuccess) {
            throw InvalidUserDataException("Token replacement didn't find any matching files! "
            + "If files are supposed to contain the tokens, check that they do not contain the token's value after a previous build error; "
            + "additionally, make sure you configured tokens to replace by configuring the <TransformTokensTask> tasks!")
        }
        backupSources.zip(processedSources).forEach { (backup, processed) ->
            println("Transforming $backup sources to $processed...")
            backup.copyRecursivelyWithTransform(processed) { s ->
                var s2 = s
                tokens.forEach { s2 = s2.replace(it.key, it.value) }
                s2
            }
            val files = processedRootDir.walkTopDown().filter { it.isFile }.toList()
            println("Transformed $files")
        }
        println("Transformed sources tokens!")
    }
}
