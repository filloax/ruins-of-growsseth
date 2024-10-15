import com.ruslan.gradle.TransformTokensTask
import com.ruslan.gradle.getFilloaxlib
import com.ruslan.gradle.getResourcefulConfig

plugins {
    // see buildSrc
    id("com.ruslan.gradle.multiloader-convention")
    id("com.ruslan.gradle.token-replacement")

	kotlin("jvm")
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.atomicfu)

	alias(libs.plugins.moddevgradle)
}

val modid: String by project
val modVersion = libs.versions.modversion.get()
val minecraftVersion = libs.versions.minecraft.asProvider().get()

// Project settings
val cydoVersion = (property("cydoVersion") as String).toBoolean()

version = "$modVersion-${minecraftVersion}-base"

base {
    archivesName = modid
}

neoForge {
	// vanilla mode, see moddevgradle docs
	neoFormVersion = libs.versions.neoform

	validateAccessTransformers = true

	parchment {
		minecraftVersion = libs.versions.parchment.minecraft
		mappingsVersion = libs.versions.parchment.asProvider()
	}

	// access transformers use default path so no need to config
}

val socketIoLibs = ext.get("socketio-libs") as List<String>

dependencies {
	compileOnly( libs.jsr305 )
	compileOnly( libs.log4j )

	compileOnly( libs.kotlin.stdlib )
	compileOnly( libs.kotlin.reflect )
	compileOnly( libs.kotlin.serialization )
	compileOnly( libs.kotlin.datetime )

    compileOnly( libs.mixin )
    compileOnly( libs.mixinextras.common )

	socketIoLibs.forEach(this::compileOnly)

	compileOnly(getResourcefulConfig())
	compileOnly(getFilloaxlib())
}

sourceSets.main.get().resources.srcDir(project(":base").file("src/generated/resources"))

// custom tasks

tasks.register<Zip>("zipEgobalegoFolder") {
	from(project.file("tools/egobalego-at-home"))
	exclude(".venv")
	destinationDirectory.set(project.file("build/egobalego-at-home"))
	archiveFileName.set("Egobalego at Home.zip")
}


val packFormats = mapOf(		// used to set the pack format inside the pack.mcmeta file of the reference datapack
	"1.20.6" to "41",
	"1.21" to "48"
)

val createDatapackTask = tasks.register("createDatapackMeta") {
	val packMeta = project.file("build/datapack/pack.mcmeta")
	packMeta.parentFile.mkdirs()
	packMeta.writeText("{\"pack\": {\"pack_format\": ${packFormats[minecraftVersion]},\"description\": \"Reference datapack for editing Growsseth data\"}}")
}

tasks.register<Zip>("makeReferenceDatapack") {
	dependsOn(createDatapackTask)

	val sourceDir = project.file("src/main/")

	from(sourceDir.resolve("generated/data/growsseth")) {
		into("data/growsseth")
	}
	include("growsseth_researcher_trades/**")

	from(sourceDir.resolve("resources/data/growsseth")) {
		into("data/growsseth")
	}
	include("growsseth_places/**", "growsseth_researcher_dialogue/**", "growsseth_researcher_trades/**", "growsseth_templates/**")

	from(project.file("build/datapack/"))
	include("pack.mcmeta")

	destinationDirectory.set(project.file("build/datapack"))
	archiveFileName.set("Reference Datapack.zip")
}


tasks.named("build") {
	dependsOn("zipEgobalegoFolder")
	dependsOn("makeReferenceDatapack")
}

tasks.withType<Jar> {
	// Cydo version: remove structure spawns
	if (cydoVersion) {
		println("Cydo version: will exclude structure spawns...")
		exclude("data/growsseth/worldgen/structure_set/**")
	}
}


// Task defined in the custom plugin in buildSrc

tasks.withType<TransformTokensTask> {
	val env = System.getenv()
	replaceTokens(mapOf(
		"$@MUSIC_PW@" to (env["GROWSSETH_MUSIC_PW"] ?: run {
			project.logger.error("Music key not set up in env variable GROWSSETH_MUSIC_PW, music in builds won't work!")
			""
		}),
	))
}
