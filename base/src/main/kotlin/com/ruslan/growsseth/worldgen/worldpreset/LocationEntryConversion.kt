package com.ruslan.growsseth.worldgen.worldpreset

import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.dialogues.DialogueLine
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

object LocationEntryConversion {
    const val KEY_CONTENT = "name"
    const val KEY_TEXT = "text"
    const val KEY_KEY = "key"

    private val JSON = Json

    // TODO: COPIED FROM DIALOGUES ONE, TO ADAPT FOR PLACES

    /**
     * returns: dialogue file with text entries replaced to keys, and dialogues lang object with the extracted text in the keys
     */
    fun extractKeysFromPlacesFile(root: JsonArray, langPrefix: String): Pair<JsonArray, JsonObject> {
        val entries: List<JsonElement> = JSON.decodeFromJsonElement(root)
        val langStrings = mutableMapOf<String, String>()
        val toKeys = entries.withIndex().map { (index, element) ->
            when (element) {
                is JsonObject -> extractKeysFromEntry(element, "${langPrefix}.$index") { k, v -> langStrings[k] = v }
                else -> element
            }
        }.let(::JsonArray)

        return Pair(toKeys, createNestedJsonObject(langStrings))
    }

    fun transformOldDialogueFile(root: JsonObject): JsonObject {
        // old format changes dialogue lines mainly, so everything else is the same
        // object containing shared + events, each field is an array containing a list of
        // dialogue entries outside of shared can have only an id, referencing a shared dialogue,
        // and can also be a string (direct text) instead of an object

        return root.mapValues { (key, dialogues) ->
            dialogues.jsonArray.map(::transformOldDialogueEntry).let(::JsonArray)
        }.let(::JsonObject)
    }

    private fun createNestedJsonObject(map: Map<String, String>, maxDepth: Int? = null): JsonObject {
        val root = mutableMapOf<String, JsonElement>()

        for ((path, value) in map) {
            val keys = path.split(".").let { maxDepth?.let { d ->
                if (d < it.size) {
                    it.subList(0, d) + it.subList(d, it.size).joinToString(".")
                } else { it }
            } ?: it }
            var currentLevel = root

            for (i in keys.indices) {
                val key = keys[i]
                if (i == keys.lastIndex) {
                    currentLevel[key] = JsonPrimitive(value)
                } else {
                    val nextLevel = (currentLevel[key] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
                    currentLevel[key] = JsonObject(nextLevel)
                    currentLevel = nextLevel
                }
            }
        }

        return JsonObject(root)
    }

    private fun extractKeysFromEntry(dialogueEntry: JsonObject, prefix: String, keysInserter: (key: String, value: String) -> Unit): JsonElement {
        val content = dialogueEntry[KEY_CONTENT] ?: return dialogueEntry

        val newContent = when (content) {
            is JsonPrimitive -> content
            is JsonObject -> extractKeysFromContent(content, prefix, keysInserter)
            is JsonArray -> if (content.size == 1 && content[0] is JsonObject) {
                extractKeysFromContent(content[0].jsonObject, prefix, keysInserter)
            } else {
                JsonArray(content.withIndex().map { (index, elem) ->
                    if (elem !is JsonObject) return@map elem
                    extractKeysFromContent(elem, "$prefix.$index", keysInserter)
                })
            }
        }
        val simplifyContent = { element: JsonElement ->
            if (element !is JsonObject)
                element
            else if (element.keys.size == 1 && element.containsKey(KEY_KEY))
                JsonPrimitive(element[KEY_KEY]!!.jsonPrimitive.content)
            else
                element
        }
        val simplifiedContent = when (newContent) {
            is JsonObject -> simplifyContent(newContent)
            is JsonArray -> newContent.map(simplifyContent).let {
                if (it.size == 1)
                    it[0]
                else JsonArray(it)
            }
            is JsonPrimitive -> newContent
        }

        // has only content
        if (dialogueEntry.keys.size == 1) {
            return simplifiedContent
        }

        return JsonObject(
            mapOf(KEY_CONTENT to simplifiedContent )
                    + dialogueEntry.minus(KEY_CONTENT)
        )
    }

    private fun extractKeysFromContent(content: JsonObject, key: String, keysInserter: (key: String, value: String) -> Unit): JsonObject {
        return if (content.containsKey(KEY_TEXT)) {
            keysInserter("${Constants.LANG_PLACES_PREFIX}.$key", content[KEY_TEXT]!!.jsonPrimitive.content)

            JsonObject(
                mapOf(KEY_KEY to JsonPrimitive(key))
                        + content.toMap().minus(KEY_TEXT)
            )
        } else { content }
    }

    private fun transformOldDialogueEntry(dialogueEntry: JsonElement): JsonObject {
        return when (dialogueEntry) {
            // single string dialogue entry -> text dialogueentry
            is JsonPrimitive -> JsonObject(mapOf(KEY_CONTENT to lineFromString(dialogueEntry.content)))
            is JsonObject -> JsonObject(dialogueEntry.mapValues { (key, value) ->
                if (key == KEY_CONTENT) {
                    Json.encodeToJsonElement(parseOldDialogueContentFormat(value).map {
                        DialogueLine(text=it.content, duration=it.duration)
                    })
                } else {
                    value
                }
            } )
            is JsonArray -> throw IllegalArgumentException("Cannot parse json arrays as dialogue entries")
        }
    }

    private fun parseOldDialogueContentFormat(element: JsonElement): List<DialogueLineDeprecated> {
        val lineArray = when (element) {
            is JsonArray -> {
                JsonArray(element.jsonArray.map(::transformOldLineItem))
            } is JsonPrimitive -> {
                JsonArray(element.jsonPrimitive.content.split("\n").map { oldLineFromString(it.trim()) })
            } is JsonObject -> {
                JsonArray(mutableListOf(transformOldLineItem(element)))
            } else -> {
                throw SerializationException("Unrecognized element $element")
            }
        }

        return Json.decodeFromJsonElement(lineArray)
    }

    private fun transformOldLineItem(element: JsonElement): JsonObject {
        return when (element) {
            is JsonObject    -> element // add additional stuff here later if needed
            is JsonPrimitive -> oldLineFromString(element.content)
            else             -> throw SerializationException("Inner element should be primitive or object, is $element")
        }
    }

    private fun lineFromString(str: String) =
        JsonObject(mutableMapOf(KEY_TEXT to JsonPrimitive(str)))

    private fun oldLineFromString(str: String) =
        JsonObject(mutableMapOf(KEY_CONTENT to JsonPrimitive(str)))

    @Serializable
    private data class DialogueLineDeprecated(
        val content: String,
        val duration: Float? = null,
    )
}