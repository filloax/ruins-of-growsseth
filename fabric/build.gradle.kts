import com.ruslan.gradle.*

plugins {
	// see buildSrc
	id("com.ruslan.gradle.multiloader-loader")

	alias(libs.plugins.loom)
}

val utils = project.utils(versionCatalogs, ext)

val modid: String by project
val modVersion = libs.versions.modversion.get()
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val parchmentMcVersion = libs.versions.parchment.minecraft.get()
val parchmentVersion = libs.versions.parchment.asProvider().get()
val includeDeps = (property("includeDeps") as String).toBoolean()

version = "$modVersion-$minecraftVersion-fabric"

if (includeDeps) println("Including dependencies for test mode")

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

		create("musicKey") {
			client()
			mainClass = "com.ruslan.growsseth.data.MusicKeyCreateKt"

			name("Create music key")
			runDir("run/musicencrypt")
		}

		create("musicEncrypt") {
			client()
			mainClass = "com.ruslan.growsseth.data.MusicEncryptKt"

			name("Encrypt music")
			runDir("run/musicencrypt")
		}

		matching{ it.name == "client" || it.name == "server" }.configureEach {
			vmArg("-Dmixin.debug.export=true")
		}
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
}

// Mod description handling (different in loaders due to formatting)
val rootDirectory = project.rootDir
val modDescriptionFile = rootDirectory.resolve("mod-description.txt")

utils.addExtraResourceProp("description",  modDescriptionFile.readText().replace("\r", "").replace("\n", "\\n"))

tasks.withType<ProcessResources>().configureEach {
	inputs.file(modDescriptionFile)
}