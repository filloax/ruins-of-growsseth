import com.ruslan.gradle.TransformTokensTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.incremental.deleteDirectoryContents

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("fabric-loom")
	id("maven-publish")
	// see buildSrc
	id("com.ruslan.gradle.token-replacement")
}

val modid: String by project
val minecraftVersion = property("minecraft_version") as String

val javaVersion: Int = (property("javaVersion")!! as String).toInt()
val kotlinVersion: String by project
val kotlinSerializationVersion: String by project
val javaVersionEnum = JavaVersion.values().find { it.majorVersion == javaVersion.toString() } ?: throw Exception("Cannot find java version for $javaVersion")

val useLocalJarFxLib = (property("useLocalJarFxLib") as String).toBoolean()
val alwaysUseLocalMavenFXLib = (property("alwaysUseLocalMavenFXLib")!! as String).toBoolean()
val fxLibVersion: String by project
val useLocalMavenFxLib = alwaysUseLocalMavenFXLib || fxLibVersion.contains(Regex("rev\\d+"))

val parchmentVersion = property("parchment_version")
val loaderVersion = property("loader_version")
val fabricKotlinVersion = property("fabric_kotlin_version")
val fabricApiVersion = property("fabric_api_version")

val modMenuVersion = property("mod_menu_version")
val resConfigVersion = property("rconfig_version")
val resConfigMcVersion = property("rconfig_mc_version")

version = property("mod_version")!! as String

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

dependencies {
	minecraft("com.mojang:minecraft:${minecraftVersion}")
	//mappings("net.fabricmc:yarn:${property("yarnMappings")}:v2")
	mappings(loom.layered() {
		officialMojangMappings()
		if ((property("parchment_version") as String).isNotBlank()) {
			parchment("org.parchmentmc.data:parchment-${minecraftVersion}:${parchmentVersion}@zip")
		}
	})

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

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

	modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${fabricKotlinVersion}")
	// include("net.fabricmc:fabric-language-kotlin:${property("fabricKotlinVersion")}")

	modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}") {
		exclude(module = "fabric-api-deprecated")
	}
	modImplementation("com.terraformersmc:modmenu:${modMenuVersion}")
	modImplementation("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-$resConfigMcVersion:$resConfigVersion")

	val fxLib = (if (useLocalJarFxLib)
			":fx-lib-${fxLibVersion}-fabric"
		else if (useLocalMavenFxLib)
			"com.filloax.fxlib:fx-lib:${fxLibVersion}-fabric"
		else
			"com.github.filloax:fx-lib:v${fxLibVersion}-fabric"
		)

	fxLib.let{
		 modImplementation(it)
		 include(it)
	}
    implementation(kotlin("stdlib-jdk8"))
}

tasks.register("zipEgobalegoFolder") {
	group = "custom"

	val sourceDir = project.file("tools/egobalego-at-home")
	val destinationDir = project.file("build/egobalego-at-home")
	val zipFile = destinationDir.resolve("Egobalego At Home.zip")

	inputs.dir(sourceDir)
	outputs.file(zipFile)

	doLast {
		destinationDir.mkdirs()
		project.exec {
			workingDir = sourceDir.parentFile
			commandLine("zip", "-r", "-x", "egobalego-at-home/.venv/**", "-o", zipFile.absolutePath, "egobalego-at-home")
		}
	}
}

tasks.register("makeReferenceDatapack") {
	group = "custom"

	val sourceDir = project.file("src/main/")

	val generatedRoot = sourceDir.resolve("generated/data/growsseth")
	val generatedDir = generatedRoot.resolve("growsseth_researcher_trades")

	val resourcesDir = sourceDir.resolve("resources/data/growsseth")
	val resourcesDirs = listOf(
		resourcesDir.resolve("growsseth_places"),
		resourcesDir.resolve("growsseth_researcher_dialogue"),
		resourcesDir.resolve("growsseth_researcher_trades"),
		resourcesDir.resolve("growsseth_templates")
	)

	val destinationDir = project.file("build/datapack")
	val zipFile = destinationDir.resolve("Reference Datapack.zip")

	val packMeta = destinationDir.resolve("pack.mcmeta")

	inputs.dir(generatedDir)
	resourcesDirs.forEach { inputs.dir(it) }
	outputs.file(zipFile)
	outputs.file(packMeta)

	doLast {
		destinationDir.mkdirs()
		destinationDir.deleteDirectoryContents()
		packMeta.writeText("{\"pack\": {\"pack_format\": 41,\"description\": \"Edits Growsseth data\"}}")
		generatedDir.copyRecursively(destinationDir.resolve("data/growsseth/growsseth_researcher_trades"))
		for (dir in resourcesDirs){
			dir.copyRecursively(destinationDir.resolve("data/growsseth/" + dir.name))
		}
		project.exec {
			workingDir = destinationDir
			commandLine("zip", "-r", "-o", zipFile.absolutePath, "data", "pack.mcmeta")
		}
		destinationDir.resolve("data").deleteRecursively()
		destinationDir.resolve(File("pack.mcmeta")).delete()
	}
}

tasks.named("build") {
	dependsOn("zipEgobalegoFolder")
	dependsOn("makeReferenceDatapack")
}

tasks.processResources {
	mapOf(
		"version" to project.version,
		"fxlib_version" to fxLibVersion,
		"rconfig_version" to resConfigVersion,
		"fabric_kotlin_version" to fabricKotlinVersion,
		"loader_version" to loaderVersion,
		"fapi_version" to fabricApiVersion,
	).also {
		filesMatching("fabric.mod.json") {
			expand(it)
		}
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
	options.release = javaVersion
	options.encoding = "UTF-8"
}
tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "$javaVersion"
}

java {
	withSourcesJar()

	sourceCompatibility = javaVersionEnum
	targetCompatibility = javaVersionEnum
}

// configure the maven publication
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// select the repositories you want to publish to
	repositories {
		// uncomment to publish to the local maven
		// mavenLocal()
	}
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "$javaVersion"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "$javaVersion"
}

// Task defined in the custom plugin in buildSrc

tasks.withType<TransformTokensTask> {
	val env = System.getenv()
	replaceTokens(mapOf(
//		"$@TEST_REPLACEMENT_WORKING@" to (env["GROWSSETH:TEST_REPLACEMENT_WORKING"] ?: "Yes, it works but no var in env! Music will be disabled!"),
		"$@MUSIC_PW@" to (env["GROWSSETH:MUSIC_PW2"] ?: run {
			project.logger.error("Music key not set up in env variable GROWSSETH:MUSIC_PW, music in builds won't work!")
			""
		}),
	))
}