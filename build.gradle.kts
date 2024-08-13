import com.ruslan.gradle.TransformTokensTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.incremental.deleteDirectoryContents

plugins {
	kotlin("jvm")
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.loom)

	// see buildSrc
	id("com.ruslan.gradle.token-replacement")
}

// Project settings
val modid: String by project
val cydoVersion = (property("cydoVersion") as String).toBoolean()
val useLocalJarFxLib = (property("useLocalJarFxLib") as String).toBoolean()
val alwaysUseLocalMavenFXLib = (property("alwaysUseLocalMavenFXLib")!! as String).toBoolean()
val includeDeps = (property("includeDeps") as String).toBoolean()

// Main versions
//val libs = project.versionCatalogs.find("libs").get()
val modVersion = property("mod_version")!! as String
val javaVersion = libs.versions.java.get()
val kotlinVersion = libs.versions.kotlin.asProvider().get()
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val parchmentMcVersion = libs.versions.parchment.minecraft.get()
val parchmentVersion = libs.versions.parchment.asProvider().get()

val javaVersionEnum = JavaVersion.values().find { it.majorVersion == javaVersion } ?: throw Exception("Cannot find java version for $javaVersion")
val jvmTargetEnum = JvmTarget.valueOf("JVM_$javaVersion")

version = "$minecraftVersion-$modVersion"

base {
	archivesName.set(property("archivesBaseName") as String)
}

repositories {
	mavenCentral()
	mavenLocal()

	maven("https://api.modrinth.com/maven")
	maven("https://maven.terraformersmc.com/releases")
	maven {
		name = "ParchmentMC"
		url = uri("https://maven.parchmentmc.org")
	}
	maven("https://jitpack.io")
	maven {
		// Location of the maven that hosts Team Resourceful's jars.
		name = "Team Resourceful Maven"
		url = uri("https://maven.teamresourceful.com/repository/maven-public/")
	}
	flatDir {
		dirs("libs")
	}
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("growsseth") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets["client"])
		}
	}

	accessWidenerPath = file("src/main/resources/growsseth.accesswidener")
}

if (includeDeps) println("Including dependencies for test mode")

dependencies {
	minecraft( libs.minecraft )

	//mappings("net.fabricmc:yarn:${property("yarnMappings")}:v2")
	mappings(loom.layered() {
		officialMojangMappings()
		if (parchmentVersion.isNotBlank()) {
			parchment("org.parchmentmc.data:parchment-${parchmentMcVersion}:${parchmentVersion}@zip")
		}
	})

	// Socketio dependencies
	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
	implementation("org.json:json:20231013")
	implementation("org.java-websocket:Java-WebSocket:1.5.4")
	implementation("com.squareup.okio:okio:3.4.0")
	implementation("com.squareup.okhttp3:okhttp:4.11.0")
	implementation("io.socket:engine.io-client:2.1.0")
	implementation("io.socket:socket.io-client:2.1.0")

	include("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
	include("org.json:json:20231013")
	include("org.java-websocket:Java-WebSocket:1.5.4")
	include("com.squareup.okio:okio:3.4.0")
	include("com.squareup.okhttp3:okhttp:4.11.0")
	include("io.socket:engine.io-client:2.1.0")
	include("io.socket:socket.io-client:2.1.0")
	// End Socketio dependencies

	implementation( libs.kotlin.serialization )

	modImplementation( libs.fabric )

	modImplementation( libs.fabric.api ) {
		exclude(module = "fabric-api-deprecated")
	}
	libs.versions.rconfig
	listOf(
		libs.fabric.kotlin,
		libs.modmenu,
		getRconfig(),
	).forEach {
		modImplementation(it)
		if (includeDeps)
			include(it)
	}

	getFxlib().let{
		modImplementation(it)
		include(it)
	}

    implementation(kotlin("stdlib-jdk8"))
}

fun getFxlib(): String {
	val fxLibVersion = libs.versions.fxlib.get()
	val useLocalMavenFxLib = alwaysUseLocalMavenFXLib || fxLibVersion.contains(Regex("rev\\d+"))

	return (if (useLocalJarFxLib)
		":fx-lib-${fxLibVersion}-fabric"
	else if (useLocalMavenFxLib)
		"com.filloax.fxlib:fx-lib:${fxLibVersion}-fabric"
	else
		"com.github.filloax:FX-Lib:v${fxLibVersion}-fabric"
	)
}

fun getRconfig(): String {
	val mcVersion = libs.versions.rconfigMc.get()
	val version = libs.versions.rconfig.get()

	return "com.teamresourceful.resourcefulconfig" +
			":resourcefulconfig-fabric-${if (mcVersion == "") minecraftVersion else mcVersion}" +
			":$version"
}


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


tasks.processResources {
	mapOf(
		"version" to project.version,
		"fxlib_version" to libs.versions.fxlib.get(),
		"rconfig_version" to libs.versions.rconfig.get(),
		"fabric_kotlin_version" to libs.versions.fabric.language.kotlin.get(),
		"loader_version" to libs.versions.fabric.asProvider().get(),
		"fapi_version" to libs.versions.fabric.api.get(),
		"cydoniaMode" to cydoVersion,
	).also { map ->
		listOf(
			"fabric.mod.json",
			"cydonia.properties",
		).forEach { filesMatching(it) {
			expand(map)
		} }
	}.forEach { (prop, value) ->
		inputs.property(prop, value)
	}
}

java.sourceSets["main"].resources {
	srcDir("src/main/generated")
}

loom {
	runs {
		create("data") {
			client()

			name("Data Generation")
			vmArg("-Dfabric-api.datagen")
			vmArg("-Dfabric-api.datagen.output-dir=${file("src/main/generated")}")
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

tasks.withType<JavaCompile> {
	options.release.set(javaVersion.toInt())
	options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
	compilerOptions {
		jvmTarget = jvmTargetEnum
	}
}

java {
	withSourcesJar()

	sourceCompatibility = javaVersionEnum
	targetCompatibility = javaVersionEnum
}

tasks.withType<Jar> {
	// Cydo version: remove structure spawns
	if (cydoVersion) {
		println("Cydo version: will exclude structure spawns...")
		exclude("data/growsseth/worldgen/structure_set/**")
	}
}

// configure the maven publication
//publishing {
//	publications {
//		create<MavenPublication>("mavenJava") {
//			from(components["java"])
//		}
//	}
//
//	// select the repositories you want to publish to
//	repositories {
//		// uncomment to publish to the local maven
//		// mavenLocal()
//	}
//}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    jvmTarget = jvmTargetEnum
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
    jvmTarget = jvmTargetEnum
}

tasks.register("modVersion") {
    println("VERSION=$modVersion")
}

// Task defined in the custom plugin in buildSrc

tasks.withType<TransformTokensTask> {
	val env = System.getenv()
	replaceTokens(mapOf(
//		"$@TEST_REPLACEMENT_WORKING@" to (env["GROWSSETH_TEST_REPLACEMENT_WORKING"] ?: "Yes, it works but no var in env! Music will be disabled!"),
		"$@MUSIC_PW@" to (env["GROWSSETH_MUSIC_PW"] ?: run {
			project.logger.error("Music key not set up in env variable GROWSSETH_MUSIC_PW, music in builds won't work!")
			""
		}),
	))
}
