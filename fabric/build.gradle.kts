import com.ruslan.gradle.addExtraResourceProp
import com.ruslan.gradle.getFilloaxlib
import com.ruslan.gradle.getResourcefulConfig
import com.ruslan.gradle.socketIoLibs

plugins {
	// see buildSrc
	id("com.ruslan.gradle.multiloader-loader")

	kotlin("jvm")
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.loom)
}

val modid: String by project

loom {
	accessWidenerPath = project(":base").file("src/main/resources/${modid}.accesswidener")
	mixin.defaultRefmapName = "${modid}.refmap.json"

	mods {
		register(modid) {
			sourceSet(sourceSets.main.get())
		}
	}
}

// Project settings
val includeDeps = (property("includeDeps") as String).toBoolean()

val modVersion = libs.versions.modversion.get()

// Main versions
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val parchmentMcVersion = libs.versions.parchment.minecraft.get()
val parchmentVersion = libs.versions.parchment.asProvider().get()

version = "$modVersion-$minecraftVersion-fabric"

if (includeDeps) println("Including dependencies for test mode")

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
		getResourcefulConfig("fabric"),
	).forEach {
		modImplementation(it)
		if (includeDeps)
			include(it)
	}

	implementation( libs.kotlin.serialization ) { exclude(module = "kotlin-stdlib") }

	getFilloaxlib("fabric").let{
		modImplementation(it) { exclude(module = "kotlin-stdlib") }
		include(it)
	}
	implementation( libs.kotlinevents )
	include( libs.kotlinevents )
}

loom {
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
	}
}

loom.runs.matching{ it.name != "datagenClient" }.configureEach {
	this.vmArg("-Dmixin.debug.export=true")
}

// Mod description handling (different in loaders due to formatting)
val rootDirectory = project.rootDir
val modDescriptionFile = rootDirectory.resolve("mod-description.txt")

project.addExtraResourceProp("description",  modDescriptionFile.readText().replace("\r", "").replace("\n", "\\n"))

tasks.withType<ProcessResources>().configureEach {
	inputs.file(modDescriptionFile)
}