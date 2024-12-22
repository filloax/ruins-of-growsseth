plugins {
    alias(libs.plugins.ideaext)
}

// For common build.gradle, do not load plugin but use catalogs directly
val modVersion: String by project

tasks.register("modVersion") {
    println("VERSION=$modVersion")
}