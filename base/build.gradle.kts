//import com.ruslan.gradle.TransformTokensTask
import com.ruslan.gradle.*

plugins {
    // see buildSrc
	id("com.ruslan.gradle.token-replacement")
	id("com.ruslan.gradle.multiloader-convention")

	alias(libs.plugins.moddevgradle)
}
val utils = project.utils(versionCatalogs, ext)

val modid: String by project
val modVersion: String by project
val versionType: String? by project
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val cydoVersion = (property("cydoVersion") as String).toBoolean()

val versionSuffix = if (versionType?.isBlank() == true) "" else "-$versionType"

version = "$modVersion-$minecraftVersion$versionSuffix-base"

base {
	archivesName = property("archives_base_name") as String
}

neoForge {
	// vanilla mode, see moddevgradle docs
	neoFormVersion = libs.versions.neoform.get()

	validateAccessTransformers = true

	parchment {
		minecraftVersion = libs.versions.parchment.minecraft
		mappingsVersion = libs.versions.parchment.asProvider()
	}

	// access transformers use default path so no need to config
}

dependencies {
	compileOnly( libs.jsr305 )
	compileOnly( libs.log4j )
	compileOnly( libs.ow.asm )

	compileOnly( libs.kotlin.stdlib )
	compileOnly( libs.kotlin.reflect )
	compileOnly( libs.kotlin.serialization )
//	compileOnly( libs.kotlin.datetime ) // Kotlin for forge has issues with external libraries https://github.com/thedarkcolour/KotlinForForge/issues/86

    compileOnly( libs.mixin )
    compileOnly( libs.mixinextras.common )

	socketIoLibs.forEach(this::compileOnly)

	compileOnly(utils.getResourcefulConfig())
	compileOnly(utils.getFilloaxlib())

	// Mod compatibility
	compileOnly(libs.lithostitched.neoforge)
}

sourceSets.main.get().resources.srcDir(project(":base").file("src/generated/resources"))

configurations {
	create(COMMON_JAVA) {
		isCanBeResolved = false
		isCanBeConsumed = true
	}
	create(COMMON_RESOURCES) {
		isCanBeResolved = false
		isCanBeConsumed = true
	}
}

artifacts {
	sourceSets.main.get().java.sourceDirectories.forEach { add(COMMON_JAVA, it) }
	sourceSets.main.get().kotlin.sourceDirectories.forEach { add(COMMON_JAVA, it) }
	sourceSets.main.get().resources.sourceDirectories.forEach { add(COMMON_RESOURCES, it) }
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

//region custom tasks

val packFormats = mapOf(		// used to set the pack format inside the pack.mcmeta file of the reference datapack
	"1.20.6" to "41",
	"1.21" to "48",
	"1.21.1" to "48"
)

val createDatapackTask = tasks.register("createDatapackMeta") {
	val packMeta = project.file("build/datapack/pack.mcmeta")
	packMeta.parentFile.mkdirs()
	packMeta.writeText("{\"pack\": {\"pack_format\": ${packFormats[minecraftVersion]}, \"description\": \"Reference datapack for editing Growsseth data\"} }")
}

tasks.register<Zip>("makeReferenceDatapack") {
	dependsOn(createDatapackTask)

	val sourceDir = project.file("src/main/")

	// Json files from generated folder
	from(sourceDir.resolve("generated/data/growsseth")) {
		into("data/growsseth")
	}
	include("growsseth_researcher_trades/**")

	// Json files from resources folder
	from(sourceDir.resolve("resources/data/growsseth")) {
		into("data/growsseth")
	}
	include("growsseth_places/**", "growsseth_researcher_dialogue/**",
		"growsseth_researcher_trades/**", "growsseth_templates/**", "lang/**")

	from(project.file("build/datapack/"))
	include("pack.mcmeta")

	destinationDirectory.set(project.file("build/datapack"))
	archiveFileName.set("Reference Datapack.zip")
}

tasks.named("build") {
	dependsOn("makeReferenceDatapack")
}
//endregion

//region Dokka
// susceptible to changes in dokka v2
listOf(
	tasks.named("dokkaGenerateModuleJavadoc"),
	tasks.named("dokkaGenerate"),
).forEach { task ->
	task {
		mustRunAfter(tasks.named("replaceTransformedSources"))
		mustRunAfter(tasks.named("restoreSources"))
	}
}
//endregion
