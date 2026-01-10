import com.ruslan.gradle.*

plugins {
    // see buildSrc
    id("com.ruslan.gradle.multiloader-loader")

    alias(libs.plugins.moddevgradle)
}

val utils = project.utils(versionCatalogs, ext)

val modid: String by project
val modVersion: String by project
val versionType: String? by project
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val parchmentMcVersion = libs.versions.parchment.minecraft.get()
val parchmentVersion = libs.versions.parchment.asProvider().get()
val includeDeps = (property("includeDeps") as String).toBoolean()

val versionSuffix = if (versionType?.isBlank() == true) "" else "-$versionType"

version = "$modVersion-$minecraftVersion$versionSuffix-neoforge"

if (includeDeps) println("Including dependencies for test mode")

neoForge {
    version = libs.versions.neoforge.asProvider().get()

    validateAccessTransformers = true
    accessTransformers.files.setFrom( project(BASE_PROJECT).file("src/main/resources/META-INF/accesstransformer.cfg"))

    parchment {
        minecraftVersion = parchmentMcVersion
        mappingsVersion = parchmentVersion
    }

    runs {
        create("growsseth_client") {
            client()
            jvmArgument("-Dmixin.debug.export=true")
        }

        create("growsseth_server") {
            server()
        }

        /*
        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modid)
        }
        */

        configureEach {
            // Recommended logging data for a userdev environment
            // The markers can be added/remove as needed separated by commas.
            // "SCAN": For mods scan.
            // "REGISTRIES": For firing of registry events.
            // "REGISTRYDUMP": For getting the contents of all registries.
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("neoforge.enabledGameTestNamespaces", modid)
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

    // Kotlin for forge has issues with external libraries https://github.com/thedarkcolour/KotlinForForge/issues/86
//    implementation( libs.kotlin.datetime.jvm )
//    jarJar( libs.kotlin.datetime.jvm )

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

    utils.getFilloaxlib("neoforge").let{
        implementation(it) { exclude(module = "kotlin-stdlib") }
        jarJar(it)
    }
    implementation( libs.kotlinevents )
    jarJar( libs.kotlinevents )

    // Mod compat
    compileOnly(libs.lithostitched.neoforge)
    compileOnly(libs.rctapi.neoforge)
    compileOnly(libs.cobblemon.neoforge)
    compileOnly(libs.endremastered.neoforge)
}


// Mod description handling (depends on loader)
val rootDirectory = project.rootDir
val modDescriptionFile = rootDirectory.resolve("mod-description.txt")

utils.addExtraResourceProp("description", modDescriptionFile.readText().replace("\r", ""))

tasks.withType<ProcessResources>().configureEach {
    inputs.file(modDescriptionFile)
}