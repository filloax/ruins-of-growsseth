package com.ruslan.growsseth.dialogues

import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.GrowssethConfig
import com.ruslan.growsseth.http.GrowssethApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller


/**
 * @param content List of dialogue lines. In JSON can have different formats, mainly:
 * - a single string
 * - a [DialogueLine] object
 * - a list of strings or [DialogueLine] objects (can mix)
 * @param weight Float weight of the value for random choice, higher is easier to choose. Defaults to 1.
 * @param id String id to identify this dialogue entry, needed for shared dialogues and dialogues that have a useLimit
 *   (to identify their amount of usages)
 * @param useLimit Maximum amount of times this specific dialogue entry can be played, regardless of total event triggers.
 *   Requires an id to be set to allow storing the amount of triggers.
 * @param afterRepeatsMin Minimum amount of event triggers for the dialogue's event for the dialogue to play.
 * @param afterRepeatsMax As [afterRepeatsMin], but max amount (inclusive).
 * @param afterCloseRepeatsMin Minimum amount of event triggers within some time (defaults to a minute, configurable in entity)
 *   for the dialogue's event for the dialogue to play.
 * @param afterCloseRepeatsMax As [afterCloseRepeatsMin], but max amount (inclusive).
 * @param requiresQuest If this dialogue requires a quest (as per [com.ruslan.growsseth.quests] package content), specify quest name.
 *   If the entity only has one quest assigned in its class, can just specify the stage instead, this will default to the one quest
 *   it has. (Currently quests only support one per entity, but future might not)
 * @param requiresQuestStage As [requiresQuest], but also require a specific quest stage by its name. Meant for
 *   dialogues that trigger after that stage is triggered.
 * @param requiresUntilQuestStage As [requiresQuestStage], but disables the dialogue instead, at the specified stage.
 *   Meant for dialogues that shouldn't risk to be played if the quest is started, or the stage is surpassed.
 * @param requiresEventParam Requires a specific value of the [DialogueEvent] param, for example the name in a RENAME event.
 *   If the event has no param (in general or in that specific trigger) will not allow the dialogue.
 * @param immediate If true, immediately play the dialogue when triggered, skipping any queue or delay.
 * @param data Arbitrary string-string data object, to be used by specific entities in their own ways.
 *   (For example, an entity might have sound customizations here).
 * @param priority Int, defaults to 0. After filtering allowed dialogues with the other attributes, will keep only the
 *   ones with the highest priority for random choice. Set this to a higher value to force this to play if the conditions
 *   are met (avoid setting this if the dialogue can always play and has no limits).
 */
@Serializable
data class DialogueEntry(
    @Serializable(with = DialogueLineStringsSerializer::class)
    val content: List<DialogueLine>,
    val weight: Float = 1f,
    val id: String? = null,
    @Serializable(with = ListStringSerializer::class)
    val groups: List<String>? = null,
    val useLimit: Int? = null,
    val groupUseLimit: Int? = null,
    val afterRepeatsMin: Int = 1, //event-wide
    val afterRepeatsMax: Int? = null,
    val afterCloseRepeatsMin: Int = 1, //event-wide
    val afterCloseRepeatsMax: Int? = null,
    val requiresQuest: String? = null, // can be omitted even if stage set, will default to the one quest the npc has
    val requiresQuestStage: String? = null,
    val requiresUntilQuestStage: String? = null,
    val requiresEventParam: String? = null,
    val immediate: Boolean = false, // Skip the queue (for things like taking damage)
    val data: Map<String, String> = mapOf(),
    val priority: Int = 0, // higher -> has priority
) {
    init {
        assert(id != null || useLimit == null) {
            "Must set an id to track a useLimit" +
            "Were content:$content id:$id useLimit:$useLimit afterMinTimes:$afterCloseRepeatsMin afterMaxTimes:$afterCloseRepeatsMax"
        }

        content.forEach {
            it.dialogue = this
        }
    }

    companion object {
        fun of(content: String): DialogueEntry {
            return DialogueEntry(content.split("\n").map{ DialogueLine(it) })
        }

        fun getAllForEvent(event: DialogueEvent): List<DialogueEntry> {
            val out = ResearcherDialogueListener.DIALOGUE_OPTIONS[GrowssethConfig.serverLanguage]?.get(event.id)?.toMutableList() ?:
                        ResearcherDialogueListener.DIALOGUE_OPTIONS[DEFAULT_LANGUAGE]?.get(event.id)?.toMutableList() ?:
                        mutableListOf()
            if (event in ResearcherDialogueApiListener.API_DIALOGUES_EVENTS) {
                out += ResearcherDialogueApiListener.API_DIALOGUES
            }
            return out
        }

        fun getWithId(id: String): DialogueEntry? {
            return ResearcherDialogueListener.BY_ID[GrowssethConfig.serverLanguage]?.get(id)?:
                    ResearcherDialogueListener.BY_ID[DEFAULT_LANGUAGE]?.get(id)
        }

        const val DEFAULT_LANGUAGE = "en_us"
    }
}

/**
 * @param content The actual content of the line.
 * @param duration Optional duration in seconds, otherwise will be calculated by the dialogue component.
 *   (Usually by WPM).
 */
@Serializable
data class DialogueLine(
    val content: String,
    val duration: Float? = null,
) {
    @Transient
    lateinit var dialogue: DialogueEntry
}

/*
Parse line list from string, list of strings, json object, list of json objects, etc.
Valid formats:
"content": "LINES\nSEPARATED BY\nNEWLINES"
"content": {
    "content": "SINGLE LINE WITH PARAMETERS",
    "duration": 2.0
}
"content": [
    "SIMPLE LINE",
    {
        "content": "LINE WITH PARAMETERS, CAN MIX BOTH",
        "duration": 1.0
    }
]
 */
class DialogueLineStringsSerializer : JsonTransformingSerializer<List<DialogueLine>>(ListSerializer(DialogueLine.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> {
                JsonArray(element.jsonArray.map(::transformItem))
            } is JsonPrimitive -> {
                JsonArray(element.jsonPrimitive.content.split("\n").map { fromString(it.trim()) })
            } is JsonObject -> {
                JsonArray(mutableListOf(transformItem(element)))
            } else -> {
                throw SerializationException("Unrecognized element $element")
            }
        }
    }

    private fun transformItem(element: JsonElement): JsonObject {
        return when (element) {
            is JsonObject    -> element // add additional stuff here later if needed
            is JsonPrimitive -> fromString(element.content)
            else             -> throw SerializationException("Inner element should be primitive or object, is $element")
        }
    }

    // Make sure "content" matches the name of the property in DialogueLine
    private fun fromString(str: String) =
        JsonObject(mutableMapOf("content" to JsonPrimitive(str)))
}

class ListStringSerializer : JsonTransformingSerializer<List<String>>(ListSerializer(String.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonPrimitive -> JsonArray(listOf(element))
            else -> {
                throw SerializationException("Unrecognized element $element")
            }
        }
    }
}

class ResearcherDialogueListener : KotlinJsonResourceReloadListener(JSON, Constants.RESEARCHER_DIALOGUE_DATA_FOLDER) {
    companion object {
        private val JSON = Json

        val DIALOGUE_OPTIONS : MutableMap<String, MutableMap<String, MutableList<DialogueEntry>>> = mutableMapOf()
        val BY_ID = mutableMapOf<String, MutableMap<String, DialogueEntry>>()
        private val SHARED_DIALOGUES : MutableMap<String, MutableMap<String, DialogueEntry>> = mutableMapOf()

        const val SHARED_KEY = "shared"
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        DIALOGUE_OPTIONS.clear()
        SHARED_DIALOGUES.clear()
        val sharedEntriesReferences = mutableMapOf<String, MutableMap<String, MutableList<String>>>()   // languages, <event names, entry ids>

        loader.forEach { (fileIdentifier, jsonElement) ->
            try {
                val split = fileIdentifier.path.split("/")
                val langCode = split[0]
                if (split.size == 2){       // if correct it will be lang_folder_name/file_name
                    DIALOGUE_OPTIONS.computeIfAbsent(langCode) { mutableMapOf() }
                    BY_ID.computeIfAbsent(langCode) { mutableMapOf() }
                    SHARED_DIALOGUES.computeIfAbsent(langCode) { mutableMapOf() }
                    sharedEntriesReferences.computeIfAbsent(langCode) { mutableMapOf() }

                    val entries: Map<String, List<JsonElement>> = JSON.decodeFromJsonElement(jsonElement)
                    entries.forEach efr@{ (event, list) ->
                        val eventSharedRefs = sharedEntriesReferences[langCode]?.computeIfAbsent(event) { mutableListOf() }

                        val currentEntries = list.mapNotNull {
                            when (it) {
                                is JsonPrimitive -> DialogueEntry.of(it.content)
                                is JsonObject -> if (isSharedReference(it)) {
                                    val id =
                                        it["id"] ?: throw IllegalStateException("Shared entries must have an id! $it")
                                    eventSharedRefs?.add(id.jsonPrimitive.content)
                                    null
                                } else {
                                    Json.decodeFromJsonElement(DialogueEntry.serializer(), it)
                                }

                                else -> throw SerializationException("Unsupported type: ${it::class}")
                            }
                        }

                        if (event == SHARED_KEY) {
                            val byId = currentEntries.associateBy {
                                it.id ?: throw SerializationException("Shared dialogue entries must have id set!")
                            }
                            SHARED_DIALOGUES[langCode]?.putAll(byId)
                            BY_ID[langCode]?.putAll(byId)
                            return@efr
                        }

                        DIALOGUE_OPTIONS[langCode]?.computeIfAbsent(event) { mutableListOf() }?.addAll(currentEntries)
                        BY_ID[langCode]?.putAll(currentEntries.filter { it.id != null }.associateBy { it.id!! })
                    }
                }
                else {
                    RuinsOfGrowsseth.LOGGER.warn("File {} was not correctly placed in a language folder, will be ignored", fileIdentifier)
                }
            } catch (e: Exception) {
                RuinsOfGrowsseth.LOGGER.error( "Growsseth: Couldn't parse dialogue file {}", fileIdentifier, e)
            }
        }

        sharedEntriesReferences[GrowssethConfig.serverLanguage]?.forEach { (event, sharedRefs) ->
            DIALOGUE_OPTIONS[GrowssethConfig.serverLanguage]?.get(event)?.addAll(sharedRefs.map {
                SHARED_DIALOGUES[GrowssethConfig.serverLanguage]?.get(it) ?: throw SerializationException("Unknown id $it of shared dialogue reference in event $event")
            })
        }
    }

    private fun isSharedReference(jsonObject: JsonObject): Boolean {
        return jsonObject.entries.size == 1 && jsonObject.containsKey("id")
    }
}

/**
 * Listener for arbitrary dialogues defined through website.
 */
object ResearcherDialogueApiListener {
    // Use this event so the dialogue triggers asap when a player is in range
    val API_DIALOGUES_EVENTS = listOf(
        BasicDialogueEvents.TICK_NEAR_PLAYER,
        BasicDialogueEvents.PLAYER_ARRIVE,
        BasicDialogueEvents.PLAYER_ARRIVE_NIGHT
    )
    private const val EVENT_PREFIX = "rdialogue"
    val API_DIALOGUES = mutableListOf<DialogueEntry>()

    fun init() {
        GrowssethApi.current.subscribe { api, server ->
            API_DIALOGUES.clear()
            val events = api.events
            val matching = events.filter { it.name.startsWith("$EVENT_PREFIX/") && it.active }
            matching.forEach { event ->
                val id = event.name.replace("$EVENT_PREFIX/", "").trim()
                val desc = event.desc
                if (desc == null) {
                    RuinsOfGrowsseth.LOGGER.error("Online event: error, no content; $event")
                    return@forEach
                }
                API_DIALOGUES.add(DialogueEntry(
                    desc.split("\n").map(::DialogueLine),
                    id = id,
                    useLimit = 1,
                    priority = 100,
                ))
            }
        }
    }
}