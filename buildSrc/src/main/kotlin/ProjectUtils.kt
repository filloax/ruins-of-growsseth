package com.ruslan.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtraPropertiesExtension
import java.lang.Runtime.Version

fun Project.utils(versionCatalogs: VersionCatalogsExtension, ext: ExtraPropertiesExtension)
    = ProjectUtils(this, versionCatalogs, ext)

/**
 * Wrapper around some gradle stuff as there's no sane way to make utils that use
 * Gradle Kotlin DSL extension methods
 */
class ProjectUtils(
    private val project: Project,
    versionCatalogs: VersionCatalogsExtension,
    private val ext: ExtraPropertiesExtension,
) {
    private val libs = versionCatalogs.find("libs").get()

    fun getFilloaxlib(loader: String = "common"): String {
        val useLocalJarFilloaxLib = (project.property("useLocalJarFilloaxLib") as String).toBoolean()
        val alwaysUseLocalMavenFilloaxLib = (project.property("alwaysUseLocalMavenFilloaxLib")!! as String).toBoolean()

        val filloaxlibVersion = libs.findVersion("filloaxlib").get().toString()
        val useLocalMavenFilloaxLib = alwaysUseLocalMavenFilloaxLib || filloaxlibVersion.contains(Regex("rev\\d+"))

        return if (useLocalJarFilloaxLib)
            ":filloaxlib-${filloaxlibVersion}-${loader}"
        else if (useLocalMavenFilloaxLib)
            "com.filloax.filloaxlib:filloaxlib-${loader}:${filloaxlibVersion}"
        else if (loader == "common")
            "com.github.filloax.filloaxlib:filloaxlib-${loader}:${filloaxlibVersion}"
        else
            "maven.modrinth:filloaxlib:${filloaxlibVersion}-${loader}"
    }

    // neoforge doesn't have mapping issues
    fun getResourcefulConfig(loader: String = "neoforge"): String {
        val mcVersion = libs.findVersion("minecraft").get().toString()
        val rmcVersion = libs.findVersion("rconfigMc").get().toString()
        val version = libs.findVersion("rconfig").get().toString()

        return "com.teamresourceful.resourcefulconfig" +
                ":resourcefulconfig-${loader}-${if (rmcVersion == "") mcVersion else rmcVersion}" +
                ":$version"
    }

    fun addExtraResourceProp(key: String, value: String) {
        val extraProps = if (ext.has("extraProps")) {
            ext["extraProps"] as MutableMap<String, String>
        } else {
            mutableMapOf<String, String>().also { this.ext["extraProps"] = it }
        }
        extraProps[key] = value
    }

    val extraResourceProps: Map<String, String> get() {
        return "extraProps".let{ if (ext.has(it)) this.ext[it] as Map<String, String> else mapOf() }
    }
}