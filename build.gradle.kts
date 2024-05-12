import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("fabric-loom")
	id("maven-publish")
}

val javaVersion: Int = (property("javaVersion")!! as String).toInt()
val kotlinVersion: String by project
val kotlinSerializationVersion: String by project
val useLocalJarFxLib = (property("useLocalJarFxLib") as String).toBoolean()
val alwaysUseLocalMavenFXLib = (property("alwaysUseLocalMavenFXLib")!! as String).toBoolean()
version = property("mod_version")!! as String
group = property("maven_group")!! as String
val modid: String by project
val fxLibVersion: String by project
val minecraftVersion = property("minecraft_version") as String

val javaVersionEnum = JavaVersion.values().find { it.majorVersion == javaVersion.toString() } ?: throw Exception("Cannot find java version for $javaVersion")
val useLocalMavenFxLib = alwaysUseLocalMavenFXLib || fxLibVersion.contains(Regex("rev\\d+"))

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
			parchment("org.parchmentmc.data:parchment-${minecraftVersion}:${property("parchment_version")}@zip")
		}
	})

	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
	implementation("org.json:json:20230618")
	implementation("org.java-websocket:Java-WebSocket:1.5.4")
	implementation("com.squareup.okio:okio:3.0.0-alpha.9")
	implementation("com.squareup.okhttp3:okhttp:4.11.0")
	implementation("io.socket:engine.io-client:2.1.0")
	implementation("io.socket:socket.io-client:2.1.0")

	include("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
	include("org.json:json:20230618")
	include("org.java-websocket:Java-WebSocket:1.5.4")
	include("com.squareup.okio:okio:3.0.0-alpha.9")
	include("com.squareup.okhttp3:okhttp:4.11.0")
	include("io.socket:engine.io-client:2.1.0")
	include("io.socket:socket.io-client:2.1.0")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

	modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

	"net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}".let{
		modImplementation(it)
		include(it)
	}

	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}") {
		exclude(module = "fabric-api-deprecated")
	}
	"com.terraformersmc:modmenu:${property("mod_menu_version")}".let{
		modImplementation(it)
		include(it)
	}
	"com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-${property("rconfig_version")}".let{
		modImplementation(it)
		include(it)
	}

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
			commandLine("zip", "-r", zipFile.absolutePath, "egobalego-at-home")
		}
	}
}

tasks.named("build") {
	dependsOn("zipEgobalegoFolder")
}

tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.version))
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