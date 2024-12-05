package com.ruslan.growsseth.dialogues

import com.filloax.fxlib.api.EventUtil
import com.filloax.fxlib.api.FxUtils
import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
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
import net.minecraft.world.level.storage.LevelResource


/**
 * Note: for dialogues file format (including shorthands to avoid writing a full DialogueEntry object), see
 * javadoc for [ResearcherDialogueListener]
 *
 * @param content List of dialogue lines. In JSON can have different formats, see [DialogueLineStringsSerializer]
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
        fun ofKey(key: String): DialogueEntry {
            return DialogueEntry(content=listOf(DialogueLine(key=key)))
        }

        fun ofText(text: String): DialogueEntry {
            return DialogueEntry(content=listOf(DialogueLine(text=text)))
        }

        fun getAllForEvent(event: DialogueEvent): List<DialogueEntry> {
            val out = ResearcherDialogueListener.DIALOGUE_OPTIONS[event.id]?.toMutableList() ?:
                        mutableListOf()
            if (event in ResearcherDialogueApiListener.API_DIALOGUES_EVENTS) {
                out += ResearcherDialogueApiListener.API_DIALOGUES
            }
            return out
        }

        fun getWithId(id: String): DialogueEntry? {
            return ResearcherDialogueListener.BY_ID[id]
        }
    }
}

/**
 * @param key Key to the lang string of the line. Must set either this or `text`.
 * @param text Hardcoded text of the line. Must set either this or `key`.
 * @param content The actual content of the line.
 * @param duration Optional duration in seconds, otherwise will be calculated by the dialogue component.
 *   (Usually by WPM).
 */
@Serializable
data class DialogueLine(
    val key: String? = null,
    val text: String? = null,
    val duration: Float? = null,
) {
    @Transient
    lateinit var dialogue: DialogueEntry

    init {
        if (key == null && text == null) {
            throw IllegalArgumentException("Cannot have both key and text null!")
        }
        if (key != null && text != null) {
            throw IllegalArgumentException("Cannot have both key and text set!")
        }
    }
}

/**
Parse line list from string, list of strings, json object, list of json objects, etc.
Valid formats, mainly two approaches:

#### Text referencing lang strings
_(suggested method)_

Note: all lang keys will be prepended with 'dialogue.', which is also the prefix
used in the **dialogue lang files** (TODO: link). This is intentional, as that system
is designed to work with this to avoid cluttering the main lang file.

_1. Simple language key dialogue._
```json
"content": {
    "key": "mod.npcname.hello",
    "duration": 2.0
}
```
Will have 1 line, taking from the lang string "dialogue.mod.npcname.hello".
Can optionally use modifiers, currently:
- `duration`: replaces duration of line in seconds instead of using configured words per minute.

_2. Multi-line dialogue shorthand._
```json
"content": {
    "key": "mod.npcname.firstGreeting",
    "numLines": 5
}
```
This will generate 5 lines, with lang keys "dialogue.mod.npcname.firstGreeting.1" to 5.
Modifiers cannot be used.

_3. Line list_
```json
"content": [
    "mod.npcname.bye1",
    {
        "key": "mod.npcname.bye2",
        "duration": 1.0
    }
]
```
A list of lines, each with the same format as 1 or 2. Can also be mixed with lines using the
_Text directly written in data files_ format below.

#### Text directly written in data files
_(not the same as old method, not recommended, mainly meant for quick testing without modifying more files)_

```json
"content": { "text": "SINGLE LINE" }
```
```json
"content": {
    "text": "SINGLE LINE WITH PARAMETERS",
    "duration": 2.0
}
```
```json
"content": [
    "mod.npcname.bye1",
    {
        "text": "LINE WITH PARAMETERS, CAN MIX BOTH",
        "duration": 1.0
    }
]
```
*/
class DialogueLineStringsSerializer : JsonTransformingSerializer<List<DialogueLine>>(ListSerializer(DialogueLine.serializer())) {
    companion object {
        const val NUM_LINES = "numLines"
        const val KEY = "key"
    }

    override fun transformDeserialize(element: JsonElement): JsonElement {
        return prefixKeys(normalizeLines(element))
    }

    private fun normalizeLines(element: JsonElement): JsonArray {
        return when (element) {
            is JsonArray -> { // array, mix and match
                JsonArray(element.jsonArray.map(::transformItem))
            } is JsonPrimitive -> { // single string key
                JsonArray(listOf(fromString(element.content.trim())))
            } is JsonObject -> {
                if (isNumLinesObj(element)) { // numLines format
                    fromNumLines(element)
                } else { // single line object
                    JsonArray(mutableListOf(transformItem(element)))
                }
            } else -> {
                throw SerializationException("Unrecognized element $element")
            }
        }
    }

    private fun prefixKeys(array: JsonArray): JsonArray {
        return JsonArray(array.map { el ->
            val obj = el.jsonObject
            obj[KEY]?.let { keyEl ->
                val key = keyEl.jsonPrimitive.content
                assertValidLangKey(key)
                val newKey = "dialogue.$key"
                JsonObject(obj + (KEY to JsonPrimitive(newKey)))
            } ?: obj
        });
    }

    private fun transformItem(element: JsonElement): JsonObject {
        return when (element) {
            is JsonObject    -> element // add additional stuff here later if needed
            is JsonPrimitive -> fromString(element.content)
            else             -> throw SerializationException("Inner element should be primitive or object, is $element")
        }
    }

    private fun isNumLinesObj(obj: JsonObject) = obj.keys.contains(NUM_LINES)

    private fun fromString(str: String) = JsonObject(mutableMapOf(
            KEY to JsonPrimitive(str),
        ))

    private fun fromNumLines(obj: JsonObject): JsonArray {
        val numLines = obj[NUM_LINES]!!.jsonPrimitive.content.toInt()
        val keyPrefix = obj[KEY]!!.jsonPrimitive.content
        return JsonArray((1 .. numLines).map { JsonObject(mapOf(
            KEY to JsonPrimitive("$keyPrefix.$it")
        )) })
    }

    private fun assertValidLangKey(key: String) {
        val pattern = Regex("^(\\w+\\.)*\\w+$")
        if (!pattern.containsMatchIn(key)) {
            throw IllegalArgumentException("Wrongly formatted lang key $key")
        }
    }
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

/**
 * The file format for dialogues is a JSON file with an object as root:
 * Each field is either the name of a dialogue event or `shared`, containing a list of [DialogueEntry].
 * Dialogues inside `shared` require the **id** field, while dialogues outside of `shared` can be replaced by one
 * of the following shorthands:
 * - A string, will create a dialogue with just that lang key as its only line
 * - An object containing only the **id** field, will reference a dialogue in the `shared` entries.
 */
class ResearcherDialogueListener : KotlinJsonResourceReloadListener(JSON, Constants.RESEARCHER_DIALOGUE_DATA_FOLDER) {
    companion object {
        private val JSON = Json {
            prettyPrint = true
        }

        val DIALOGUE_OPTIONS = mutableMapOf<String, MutableList<DialogueEntry>>()
        val BY_ID = mutableMapOf<String, DialogueEntry>()
        private val SHARED_DIALOGUES = mutableMapOf<String, DialogueEntry>()

        const val SHARED_KEY = "shared"
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        DIALOGUE_OPTIONS.clear()
        SHARED_DIALOGUES.clear()
        val sharedEntriesReferences = mutableMapOf<String, MutableList<String>>()   // <event names, entry ids>

        loader.forEach { (fileIdentifier, jsonElement) ->
            try {
                val entries: Map<String, List<JsonElement>> = JSON.decodeFromJsonElement(jsonElement)
                entries.forEach efr@{ (event, list) ->
                    val eventSharedRefs = sharedEntriesReferences.computeIfAbsent(event) { mutableListOf() }

                    val currentEntries = list.mapNotNull {
                        when (it) {
                            is JsonPrimitive -> DialogueEntry.ofKey(it.content)
                            is JsonObject -> if (isSharedReference(it)) {
                                val id = it["id"] ?: throw IllegalStateException("Shared entries must have an id! $it")
                                eventSharedRefs.add(id.jsonPrimitive.content)
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
                        SHARED_DIALOGUES.putAll(byId)
                        BY_ID.putAll(byId)
                        return@efr
                    }

                    DIALOGUE_OPTIONS.computeIfAbsent(event) { mutableListOf() }.addAll(currentEntries)
                    BY_ID.putAll(currentEntries.filter { it.id != null }.associateBy { it.id!! })
                }
            } catch (e: Exception) {
                RuinsOfGrowsseth.LOGGER.warn("Could not parse dialogue file {}, trying to convert it from old format", fileIdentifier)
                val success = try {
                    convertOldFormat(fileIdentifier, jsonElement)
                } catch (e2: Exception) {
                    RuinsOfGrowsseth.LOGGER.error( "Growsseth: Couldn't convert old dialogue file {}", fileIdentifier, e2)
                    return
                }
                if (!success) {
                    RuinsOfGrowsseth.LOGGER.error( "Growsseth: Couldn't parse dialogue file {}", fileIdentifier, e)
                }
            }
        }

        sharedEntriesReferences.forEach { (event, sharedRefs) ->
            DIALOGUE_OPTIONS[event]?.addAll(sharedRefs.map {
                SHARED_DIALOGUES[it] ?: throw SerializationException("Unknown id $it of shared dialogue reference in event $event")
            })
        }
    }

    private fun convertOldFormat(fileIdentifier: ResourceLocation, jsonElement: JsonElement): Boolean {
        val converted = try {
            DialogueEntryConversion.transformOldDialogueFile(jsonElement.jsonObject)
        } catch(e: Exception) {
            return false
        }

        EventUtil.runAtNextServerTickStart { server ->
            val generated = server.getWorldPath(LevelResource.GENERATED_DIR).normalize();
            val convertedDir = generated.resolve(Constants.RESEARCHER_DIALOGUE_CONVERTED_FOLDER)
            // sometimes the filename passed has a dot at the end, sometimes not
            val nameWithExtension = "${fileIdentifier.path}.jsonc".replace("..jsonc", ".jsonc")
            val outputFile = convertedDir.resolve(fileIdentifier.namespace).resolve(nameWithExtension)
            outputFile.parent.toFile().mkdirs()

            outputFile.toFile().writeText(Json.encodeToString(JsonElement.serializer(), converted))
            RuinsOfGrowsseth.LOGGER.warn("Saved old dialogue file $fileIdentifier to $outputFile")
        }

        RuinsOfGrowsseth.LOGGER.warn("Converted old dialogue file $fileIdentifier! Will save on server start")
        return true
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
                    desc.split("\n").map{ DialogueLine(text=it) },
                    id = id,
                    useLimit = 1,
                    priority = 100,
                ))
            }
        }
    }
}