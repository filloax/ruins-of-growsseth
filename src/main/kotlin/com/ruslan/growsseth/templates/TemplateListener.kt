package com.ruslan.growsseth.templates

import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import com.filloax.fxlib.api.withNullableDefault
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.GrowssethConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import java.awt.print.Book

private val JSON = Json

typealias ReloadAction<T> = (currentLangEntries: Map<String, T>, currentLangKeys: Set<String>, allEntries: Map<String, Map<String, T>>) -> Unit

object TemplateListener : KotlinJsonResourceReloadListener(JSON, Constants.TEMPLATE_FOLDER) {

    // Template tables are created in such a way that they automatically try getting a sample from the default language
    // if you try to get a template from a language
    private val TEMPLATES: MutableMap<TemplateKind, MutableMap<String, MutableMap<String, BookData>>> = mutableMapOf()

    // TODO: generalize better when signs are added
    private val reloadActions = mutableMapOf<TemplateKind, MutableList<ReloadAction<BookData>>>()

    fun books() = books(GrowssethConfig.serverLanguage)
    fun books(lang: String): Map<String, BookData> = TEMPLATES[TemplateKind.BOOK]!![lang] ?: TEMPLATES[TemplateKind.BOOK]!![Constants.DEFAULT_LANGUAGE]
        ?: throw Exception("No default language (${Constants.DEFAULT_LANGUAGE}) books!")

    /**
     * Subscribe to the reload of templates, action will be run every time they are loaded.
     * currentLangKeys param in action is useful to check keys included both in current lang and
     * missing from current lang but present in default lang
     */
    fun onReload(kind: TemplateKind, action: ReloadAction<BookData>) {
        val actions = reloadActions.computeIfAbsent(kind) { mutableListOf() }
        actions.add(action)
    }

    enum class TemplateKind(val path: String) {
        BOOK("book")
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        val loaderEntriesByKindAndLanguage = loader.toList()
            .groupBy { (fileIdentifier, _) -> kindFromString(fileIdentifier.path.split("/")[0]) }
            .mapValues { (_, entries) -> entries.groupBy { (fileIdentifier, _) -> fileIdentifier.path.split("/")[1] } } // Lang

        TemplateKind.entries.forEach { kind ->
            // TODO: change loading when signs are added (common template data class with children? something else?)
            val langTemplates = mutableMapOf<String, MutableMap<String, BookData>>()
            TEMPLATES[kind] = langTemplates

            loaderEntriesByKindAndLanguage[kind]?.let { byLanguage ->
                val forDefaultLang = byLanguage[Constants.DEFAULT_LANGUAGE]
                    ?: throw SerializationException("No entries for kind $kind for default language ${Constants.DEFAULT_LANGUAGE}")
                processKindAndLanguageEntries(forDefaultLang, kind, Constants.DEFAULT_LANGUAGE, langTemplates)

                // Do other languages after, so default lang is already loaded
                byLanguage.keys.minus(Constants.DEFAULT_LANGUAGE).forEach { langCode ->
                    processKindAndLanguageEntries(byLanguage[langCode]!!, kind, langCode, langTemplates, langTemplates[Constants.DEFAULT_LANGUAGE]!!)
                }
            }

            val defaultLangTemplates = langTemplates[Constants.DEFAULT_LANGUAGE]!!

            reloadActions[kind]?.forEach {
                it(
                    langTemplates.getOrDefault(GrowssethConfig.serverLanguage, defaultLangTemplates),
                    langTemplates.keys + defaultLangTemplates.keys,
                    langTemplates,
                )
            }
        }
    }

    private fun processKindAndLanguageEntries(
        entries: List<Pair<ResourceLocation, JsonElement>>,
        kind: TemplateKind,
        langCode: String,
        langTemplates: MutableMap<String, MutableMap<String, BookData>>,
        defaultLangTemplates: Map<String, BookData>? = null,
    ) {
        entries.forEach { (fileIdentifier, jsonElement) ->
            try {
                val entry: BookData = JSON.decodeFromJsonElement(BookData.serializer(), jsonElement)
                val split = fileIdentifier.path.split("/")
                // template id is file name
                val templateId = split.subList(2, split.size).joinToString("/")
                val books = langTemplates.computeIfAbsent(langCode) {
                    if (defaultLangTemplates != null) {
                        mutableMapOf<String, BookData>().withNullableDefault { defaultLangTemplates[it] }
                    } else {
                        mutableMapOf()
                    }
                }
                val existing = books.put(templateId, entry)
                if (existing != null) {
                    RuinsOfGrowsseth.LOGGER.warn("Book template $templateId inserted but already existed: $existing")
                }
            } catch (e: Exception) {
                throw SerializationException("Growsseth: Couldn't parse book template file $fileIdentifier", e)
            }

        }
    }

    private fun kindFromString(str: String): TemplateKind {
        return TemplateKind.entries.find { it.path == str } ?: throw Exception("Entry kind folder $str not recognized")
    }
}