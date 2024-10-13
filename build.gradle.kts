plugins {
}

// For common build.gradle, do not load plugin but use catalogs directly
val libs = project.versionCatalogs.find("libs")
val modVersion = libs.get().findVersion("modversion").get()

tasks.register("modVersion") {
    println("VERSION=$modVersion")
}