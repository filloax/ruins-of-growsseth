import com.ruslan.gradle.*

plugins {
    id("com.ruslan.gradle.multiloader-loader")

    alias(libs.plugins.moddevgradle)

    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.atomicfu)
}

val utils = project.utils(versionCatalogs, ext)

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

val baseProject = project(BASE_PROJECT)

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

    socketIoLibs.forEach {
        implementation(it)
        jarJar(it)
    }

    listOf(
        libs.kotlinforge,
        utils.getResourcefulConfig("neoforge"),
    ).forEach {
        implementation(it)
        if (includeDeps)
            jarJar(it)
    }

    implementation( libs.kotlin.serialization ) { exclude(module = "kotlin-stdlib") }

    utils.getFilloaxlib("neoforge").let{
        implementation(it) { exclude(module = "kotlin-stdlib") }
        jarJar(it)
    }
    implementation( libs.kotlinevents )
    jarJar( libs.kotlinevents )
}


// Mod description handling (depends on loader)
val rootDirectory = project.rootDir
val modDescriptionFile = rootDirectory.resolve("mod-description.txt")

utils.addExtraResourceProp("description", modDescriptionFile.readText().replace("\r", ""))

tasks.withType<ProcessResources>().configureEach {
    inputs.file(modDescriptionFile)
}