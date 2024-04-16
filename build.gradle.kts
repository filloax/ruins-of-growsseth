import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("fabric-loom") version "1.6-SNAPSHOT"
	id("maven-publish")
}

val kotlinVersion: String by project
val tryUseLocalFXLib = (property("tryUseLocalFXLib")!! as String).toBoolean()
version = property("mod_version")!! as String
group = property("maven_group")!! as String
val modid: String by project

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
	minecraft("com.mojang:minecraft:${property("minecraft_version")}")
	//mappings("net.fabricmc:yarn:${property("yarnMappings")}:v2")
	mappings(loom.layered() {
		officialMojangMappings()
		if ((property("parchment_version") as String).isNotBlank()) {
			parchment("org.parchmentmc.data:parchment-${property("minecraft_version")}:${property("parchment_version")}@zip")
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

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

	modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
	// include("net.fabricmc:fabric-language-kotlin:${property("fabricKotlinVersion")}")

	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}") {
		exclude(module = "fabric-api-deprecated")
	}
	modImplementation("com.terraformersmc:modmenu:${property("mod_menu_version")}")
	modImplementation("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-${property("minecraft_version")}:${property("rconfig_version")}")

	if (!tryUseLocalFXLib) {
		modImplementation("com.github.filloax:fx-lib:v${property("fxLibVersion")}-fabric")
	} else {
		println("Loading FX-Lib from local Maven...")
		modImplementation("com.filloax.fxlib:fxlib-fabric-${property("minecraft_version")}:${property("fxLibVersion")}")
	}
    implementation(kotlin("stdlib-jdk8"))
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
	options.release = 17
	options.encoding = "UTF-8"
	options.isFork = true	// fix for https://github.com/FabricMC/fabric-loom/issues/369
}
tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "17"
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
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
    jvmTarget = "17"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "17"
}