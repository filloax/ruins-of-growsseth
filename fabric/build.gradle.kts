plugins {
	// see buildSrc
	id("com.ruslan.gradle.multiloader-convention")

	kotlin("jvm")
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.loom)
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

// Project settings
val modid: String by project
val useLocalJarFxLib = (property("useLocalJarFxLib") as String).toBoolean()
val alwaysUseLocalMavenFXLib = (property("alwaysUseLocalMavenFXLib")!! as String).toBoolean()
val includeDeps = (property("includeDeps") as String).toBoolean()

val modVersion = libs.versions.modversion.get()

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

// Main versions
//val libs = project.versionCatalogs.find("libs").get()
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val parchmentMcVersion = libs.versions.parchment.minecraft.get()
val parchmentVersion = libs.versions.parchment.asProvider().get()

version = "$minecraftVersion-$modVersion-fabric"

base {
	archivesName = modid
}

val baseProject = project(":base")

if (includeDeps) println("Including dependencies for test mode")

dependencies {
	minecraft( libs.minecraft )
	implementation( libs.jsr305 )

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

	modImplementation( libs.fabric )
	modImplementation( libs.fabric.api ) {
		exclude(module = "fabric-api-deprecated")
	}

	listOf(
		libs.fabric.kotlin,
		libs.modmenu,
		getRconfig(),
	).forEach {
		modImplementation(it)
		if (includeDeps)
			include(it)
	}

	implementation( libs.kotlin.serialization ) { exclude(module = "kotlin-stdlib") }

	getFxlib().let{
		modImplementation(it) { exclude(module = "kotlin-stdlib") }
		include(it)
	}
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

tasks.compileJava {
	source(baseProject.sourceSets.getByName("main").allSource)
}

val preCompileTasks = listOf("restoreSourcesKotlin", "restoreSourcesJava")
	.map { baseProject.tasks.getByName(it) }

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
