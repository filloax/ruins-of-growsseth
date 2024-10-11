plugins {
    // Required for NeoGradle, it's not in this template but it's here if you want to add it.
    alias(libs.plugins.ideaext)
}

// For common build.gradle, do not load plugin but use catalogs directly
val libs = project.versionCatalogs.find("libs")
val modVersion = libs.get().findVersion("modversion").get()

tasks.register("modVersion") {
    println("VERSION=$modVersion")
}