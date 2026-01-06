import com.ruslan.gradle.*

plugins {
	// see buildSrc
	id("com.ruslan.gradle.multiloader-loader")

	alias(libs.plugins.loom)
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

val isDataRun = gradle.startParameter.taskNames.any { it.contains("runData") }
val cobblemonTest = isDataRun || (property("cobblemonTest") as String).toBoolean()

version = "$modVersion-$minecraftVersion$versionSuffix-fabric"

if (includeDeps) println("Including dependencies for test mode")
if (cobblemonTest) println("Using cobblemon-related dependencies at runtime")

loom {
	accessWidenerPath = project(BASE_PROJECT).file("src/main/resources/${modid}.accesswidener")
	mixin.defaultRefmapName = "${modid}.refmap.json"

	mods {
		register(modid) {
			sourceSet(sourceSets.main.get())
		}
	}

	runs {
		create("data") {
			client()

			name("Data Generation")
			vmArg("-Dfabric-api.datagen")
			vmArg("-Dfabric-api.datagen.output-dir=${file("../base/src/generated/resources")}")
			vmArg("-Dfabric-api.datagen.modid=${modid}")

			runDir("build/datagen")
		}

		create("growssethMusicKeyCreate") {
			client()
			mainClass = "com.ruslan.growsseth.data.MusicKeyCreateKt"

			name("Create music key")
			runDir("../music-encrypt")
		}

		create("growssethMusicEncrypt") {
			client()
			mainClass = "com.ruslan.growsseth.data.MusicEncryptKt"

			name("Encrypt music")
			runDir("../music-encrypt")
		}

		matching{ it.name == "client" || it.name == "server" }.configureEach {
			vmArg("-Dmixin.debug.export=true")
		}
	}
}

if (cobblemonTest) {
    repositories {
        maven { setUrl("https://maven.architectury.dev/") }
        maven { setUrl("https://maven.wispforest.io") }
    }
}

dependencies {
	minecraft( libs.minecraft )
	implementation( libs.jsr305 )
	mappings(loom.layered() {
		officialMojangMappings()
		if (parchmentVersion.isNotBlank()) {
			parchment("org.parchmentmc.data:parchment-${parchmentMcVersion}:${parchmentVersion}@zip")
		}
	})

	socketIoLibs.forEach {
		implementation(it)
		include(it)
	}

	modImplementation( libs.fabric )
	modImplementation( libs.fabric.api ) {
		exclude(module = "fabric-api-deprecated")
	}

	listOf(
		libs.fabric.kotlin,
		libs.modmenu,
		utils.getResourcefulConfig("fabric"),
	).forEach {
		modImplementation(it)
		if (includeDeps)
			include(it)
	}

	implementation( libs.kotlin.serialization ) { exclude(module = "kotlin-stdlib") }

	utils.getFilloaxlib("fabric").let{
		modImplementation(it) { exclude(module = "kotlin-stdlib") }
		include(it)
	}
	implementation( libs.kotlinevents )
	include( libs.kotlinevents )

	// Mod compat
	if (false) {
		modCompileOnly(libs.lithostitched.fabric)
		modCompileOnly(libs.cobblemon.fabric)
		modCompileOnly(libs.rctapi.fabric)
		modCompileOnly(libs.endremastered)
	}

    // For datagen
	if (false) {
		modCompileOnly(libs.megashowdown.fabric)
		modCompileOnly(libs.architectury.fabric)
	}

    if (cobblemonTest) {
        modLocalRuntime("io.wispforest:accessories-fabric:1.1.0-beta.52+1.21.1")

        modLocalRuntime("dev.architectury:architectury-fabric:13.0.8")

        modLocalRuntime(libs.cobblemon.fabric)
        modLocalRuntime(libs.rctapi.fabric)
        modLocalRuntime(libs.megashowdown.fabric)

        modLocalRuntime("maven.modrinth:cobblemontools:cvHY7gmJ")
        modLocalRuntime("maven.modrinth:rib:C4vWmZoB")
        modLocalRuntime("maven.modrinth:luckperms:l47d4ZWk")
        modLocalRuntime("maven.modrinth:fabric-permissions-api:62DUD085")
        modLocalRuntime("net.kyori:adventure-platform-fabric:5.14.0")
        modLocalRuntime("net.kyori:adventure-text-serializer-gson:4.17.0")
        modLocalRuntime("net.kyori:adventure-text-serializer-legacy:4.17.0")

        modLocalRuntime("io.wispforest:owo-lib:0.12.15+1.21")
    }
}

// Mod description handling (different in loaders due to formatting)
val rootDirectory = project.rootDir
val modDescriptionFile = rootDirectory.resolve("mod-description.txt")

utils.addExtraResourceProp("description",  modDescriptionFile.readText().replace("\r", "").replace("\n", "\\n"))

tasks.withType<ProcessResources>().configureEach {
	inputs.file(modDescriptionFile)
}