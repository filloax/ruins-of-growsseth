package com.ruslan.growsseth.templates

import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import com.filloax.fxlib.api.withNullableDefault
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.GrowssethConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

private val JSON = Json

typealias ReloadAction<T> = (currentLangEntries: Map<String, T>, currentLangKeys: Set<String>, allEntries: Map<String, Map<String, T>>) -> Unit

object TemplateListener : KotlinJsonResourceReloadListener(JSON, Constants.TEMPLATE_FOLDER) {

    // Template tables are created in such a way that they automatically try getting a sample from the default language
    // if you try to get a template from a language
    private val TEMPLATES: MutableMap<TemplateKind<out TemplateData>,MutableMap<String, out MutableMap<String, out TemplateData>>> = mutableMapOf()

    private val reloadActions = mutableListOf<ReloadEntry<out TemplateData>>()

    fun <T:TemplateData> templates(kind: TemplateKind<T>, lang: String): Map<String, T> {
        @Suppress("UNCHECKED_CAST")
        return (TEMPLATES[kind] as Map<String, Map<String, T>>?)!!.let { templates ->
            templates[lang]
                ?: templates[Constants.DEFAULT_LANGUAGE]
                ?: throw Exception("No default language (${Constants.DEFAULT_LANGUAGE}) books!")
        }
    }

    fun books() = books(com.ruslan.growsseth.config.GrowssethConfig.serverLanguage)
    fun books(lang: String): Map<String, BookData> = templates(TemplateKind.BOOK, lang)

    fun signs() = signs(com.ruslan.growsseth.config.GrowssethConfig.serverLanguage)
    fun signs(lang: String): Map<String, SignData> = templates(TemplateKind.SIGN, lang)

    /**
     * Subscribe to the reload of templates, action will be run every time they are loaded.
     * currentLangKeys param in action is useful to check keys included both in current lang and
     * missing from current lang but present in default lang
     */
    fun <T : TemplateData> onReload(kind: TemplateKind<T>, action: ReloadAction<T>) {
        reloadActions.add(ReloadEntry(kind, action))
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        val loaderEntriesByKindAndLanguage = loader.toList()
            .groupBy { (fileIdentifier, _) -> kindFromString(fileIdentifier.path.split("/")[0]) }
            .mapValues { (_, entries) -> entries.groupBy { (fileIdentifier, _) -> fileIdentifier.path.split("/")[1] } } // Lang

        TemplateKind.all.values.forEach { kind ->
            val langTemplates = loaderEntriesByKindAndLanguage[kind]?.let { reloadTemplateKind(kind, it) } ?: mutableMapOf()
            TEMPLATES[kind] = langTemplates
        }
    }

    private fun <T : TemplateData> reloadTemplateKind(kind: TemplateKind<T>, loaderEntriesByLanguage: Map<String, List<Pair<ResourceLocation, JsonElement>>>)
        : MutableMap<String, MutableMap<String, T>>
    {
        val langTemplates = mutableMapOf<String, MutableMap<String, T>>()

        val forDefaultLang = loaderEntriesByLanguage[Constants.DEFAULT_LANGUAGE]
            ?: throw SerializationException("No entries for kind $kind for default language ${Constants.DEFAULT_LANGUAGE}")
        processKindAndLanguageEntries(forDefaultLang, kind, Constants.DEFAULT_LANGUAGE, langTemplates)

        // Do other languages after, so default lang is already loaded
        loaderEntriesByLanguage.keys.minus(Constants.DEFAULT_LANGUAGE).forEach { langCode ->
            processKindAndLanguageEntries(loaderEntriesByLanguage[langCode]!!, kind, langCode, langTemplates, langTemplates[Constants.DEFAULT_LANGUAGE]!!)
        }

        val defaultLangTemplates = langTemplates[Constants.DEFAULT_LANGUAGE]!!

        reloadActions.forEach {
            if (it.kind == kind) {
                @Suppress("UNCHECKED_CAST")
                val action = it.action as ReloadAction<T>
                action(
                    langTemplates.getOrDefault(com.ruslan.growsseth.config.GrowssethConfig.serverLanguage, defaultLangTemplates),
                    langTemplates.keys + defaultLangTemplates.keys,
                    langTemplates,
                )
            }
        }

        //TEMPLATES[kind] = langTemplates
        return langTemplates
    }

    private fun <T : TemplateData> processKindAndLanguageEntries(
        entries: List<Pair<ResourceLocation, JsonElement>>,
        kind: TemplateKind<T>,
        langCode: String,
        langTemplates: MutableMap<String, MutableMap<String, T>>,
        defaultLangTemplates: Map<String, T>? = null,
    ) {
        entries.forEach { (fileIdentifier, jsonElement) ->
            try {
                val entry: T = JSON.decodeFromJsonElement(kind.serializer, jsonElement)
                val split = fileIdentifier.path.split("/")
                // template id is file name
                val templateId = split.subList(2, split.size).joinToString("/")
                val templates = langTemplates.computeIfAbsent(langCode) {
                    if (defaultLangTemplates != null) {
                        mutableMapOf<String, T>().withNullableDefault { defaultLangTemplates[it] }
                    } else {
                        mutableMapOf()
                    }
                }
                val existing = templates.put(templateId, entry)
                if (existing != null) {
                    RuinsOfGrowsseth.LOGGER.warn("Template $templateId inserted but already existed: $existing")
                }
            } catch (e: Exception) {
                throw SerializationException("Growsseth: Couldn't parse template file $fileIdentifier", e)
            }

        }
    }

    data class ReloadEntry<T : TemplateData>(val kind: TemplateKind<T>, val action: ReloadAction<T>)

    private fun kindFromString(str: String): TemplateKind<out TemplateData> {
        return TemplateKind.fromPath(str)
    }
}