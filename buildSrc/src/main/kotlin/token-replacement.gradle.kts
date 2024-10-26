package com.ruslan.gradle

import gradle.kotlin.dsl.accessors._258ea003d60887ae6eb7b6ddf797da1b.compileKotlin
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private val BACKUP_AND_TRANSFORM = "backupAndTransformSources"
private val REPLACE_TRANSFORMED = "replaceTransformedSources"
private val RESTORE = "restoreSources"

// This should only be applied to one project

println("Applying token transformer plugin...")

val tokenReplaceDirs = TokenReplaceDirs(project)

project.tasks.register<TransformTokensTask>(BACKUP_AND_TRANSFORM)

// Separate task so gradle handles inputs/outputs controls
val replaceTransformedSources by tasks.registering {
    dependsOn(BACKUP_AND_TRANSFORM)

    tokenReplaceDirs.mainSources.forEach { if (it.exists()) outputs.dir(it) }

    doLast {
        tokenReplaceDirs.processedSources.zip(tokenReplaceDirs.mainSources).forEach { (processed, main) ->
            if (processed.exists()) // maybe had only java or only kotlin
                processed.copyRecursively(main, overwrite = true)
        }
        println("Replaced base sources! (If something goes wrong later, check if they are using the replaced values, and revert if necessary!)")
    }
}

val restoreSourcesJava by tasks.registering(Copy::class) {
    dependsOn(tasks.withType<JavaCompile>())
    dependsOn(tasks.withType<KotlinCompile>())
    from(tokenReplaceDirs.backupSources[0])
    into(tokenReplaceDirs.mainSources[0])
}
val restoreSourcesKotlin by tasks.registering(Copy::class) {
    dependsOn(tasks.withType<JavaCompile>())
    dependsOn(tasks.withType<KotlinCompile>())
    from(tokenReplaceDirs.backupSources[1])
    into(tokenReplaceDirs.mainSources[1])
}
val restoreSources by tasks.registering {
    dependsOn(restoreSourcesJava)
    dependsOn(restoreSourcesKotlin)
}

afterEvaluate {
    tasks.withType<JavaCompile> {
        dependsOn(REPLACE_TRANSFORMED)
    }
    tasks.withType<KotlinCompile> {
        dependsOn(REPLACE_TRANSFORMED)
    }

    println("Applied token transformer plugin!")
}

val currentProject = project

gradle.projectsEvaluated {
    val compileTasks = mutableListOf<Task>()

    gradle.allprojects {
        compileTasks.addAll(tasks.withType<KotlinCompile>())
        compileTasks.addAll(tasks.withType<JavaCompile>())

        if (this == rootProject) return@allprojects

        tasks.withType<Javadoc> {
            dependsOn(restoreSources)
        }

        tasks.named("kotlinSourcesJar") {
            dependsOn(restoreSources)
        }

        tasks.named("sourcesJar") {
            dependsOn(restoreSources)
        }

        tasks.withType<DokkaTask> {
            dependsOn(restoreSources)
        }
        println("Applied token restore tasks to project $name")
    }

    compileTasks.forEach {
        restoreSourcesJava.get().mustRunAfter(it)
        restoreSourcesKotlin.get().mustRunAfter(it)
    }
}