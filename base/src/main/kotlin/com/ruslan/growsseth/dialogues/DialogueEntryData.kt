package com.ruslan.growsseth.dialogues

import com.filloax.fxlib.api.EventUtil
import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.http.GrowssethApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.storage.LevelResource



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

        EventUtil.runWhenServerStarted { server ->
            val generated = server.getWorldPath(LevelResource.GENERATED_DIR).normalize()
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
                val newKey = "${Constants.LANG_DIALOGUE_PREFIX}.$key"
                JsonObject(obj + (KEY to JsonPrimitive(newKey)))
            } ?: obj
        });
    }

    private fun transformItem(element: JsonElement): JsonObject {
        return when (element) {
            is JsonObject -> element // add additional stuff here later if needed
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