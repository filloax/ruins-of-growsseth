import com.ruslan.gradle.*

plugins {
    id("com.ruslan.gradle.multiloader-convention")

    alias(libs.plugins.moddevgradle)

    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.atomicfu)
}
neoForge {
    version.set(libs.versions.neoforge.asProvider())
}

val modid: String by project
val modVersion = libs.versions.modversion.get()
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val includeDeps = (property("includeDeps") as String).toBoolean()

version = "$modVersion-${minecraftVersion}-neoforge"

repositories {
    maven("https://api.modrinth.com/maven")
    maven("https://maven.terraformersmc.com/releases")
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

base {
    archivesName = property("archives_base_name") as String
}

val baseProject = project(":base")

if (includeDeps) println("Including dependencies for test mode")

neoForge {
    version.set(libs.versions.neoforge.asProvider())

    validateAccessTransformers = true
    accessTransformers.files.setFrom(baseProject.file("src/main/resources/META-INF/accesstransformer.cfg"))

    parchment {
        minecraftVersion = libs.versions.parchment.minecraft
        mappingsVersion = libs.versions.parchment.asProvider()
    }

    runs {
        create("client") {
            client()

            // Comma-separated list of namespaces to load gametests from. Empty = all namespaces.
            systemProperty("neoforge.enabledGameTestNamespaces", modid)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modid)
        }

//        create("gameTestServer") {
//            type = "gameTestServer"
//            systemProperty("neoforge.enabledGameTestNamespaces", modid)
//        }

        // applies to all the run configs above
        configureEach {
            // Recommended logging data for a userdev environment
            // The markers can be added/remove as needed separated by commas.
            // "SCAN": For mods scan.
            // "REGISTRIES": For firing of registry events.
            // "REGISTRYDUMP": For getting the contents of all registries.
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modid) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation( libs.jsr305 )
    implementation( libs.kotlin.serialization )

    implementation( libs.kotlin.reflect )
    implementation( libs.kotlin.serialization )
    implementation( libs.kotlin.datetime )

    compileOnly(baseProject)

    socketIoLibs.forEach {
        implementation(it)
        jarJar(it)
    }

    listOf(
        libs.kotlinforge,
        getResourcefulConfig("neoforge"),
    ).forEach {
        implementation(it)
        if (includeDeps)
            jarJar(it)
    }

    implementation( libs.kotlin.serialization ) { exclude(module = "kotlin-stdlib") }

    getFilloaxlib("neoforge").let{
        implementation(it) { exclude(module = "kotlin-stdlib") }
        jarJar(it)
    }
    implementation( libs.kotlinevents )
    jarJar( libs.kotlinevents )
}

tasks.compileJava {
    source(baseProject.sourceSets.getByName("main").allSource)
}

//val preCompileTasks = listOf("restoreSourcesKotlin", "restoreSourcesJava")
//    .map { baseProject.tasks.getByName(it) }
val preCompileTasks = listOf<Task>()

tasks.compileKotlin  {
    preCompileTasks.forEach { dependsOn(it) }
    source(baseProject.sourceSets.getByName("main").allSource)
}

tasks.getByName<Jar>("sourcesJar") {
    preCompileTasks.forEach { dependsOn(it) }

    val mainSourceSet = baseProject.sourceSets.getByName("main")
    from(mainSourceSet.allSource)
}
tasks.kotlinSourcesJar {
    preCompileTasks.forEach { dependsOn(it) }

    val mainSourceSet = baseProject.sourceSets.getByName("main")
    from(mainSourceSet.allSource)
}

tasks.withType<Javadoc>().configureEach {
    source(baseProject.sourceSets.getByName("main").allJava)
}

tasks.processResources {
    from(baseProject.sourceSets.getByName("main").resources)
}


// Mod description handling
val rootDirectory = project.rootDir
val modDescriptionFile = rootDirectory.resolve("mod-description.txt")

project.addExtraResourceProp("description", modDescriptionFile.readText().replace("\r", ""))

tasks.withType<ProcessResources>().configureEach {
    inputs.file(modDescriptionFile)
}