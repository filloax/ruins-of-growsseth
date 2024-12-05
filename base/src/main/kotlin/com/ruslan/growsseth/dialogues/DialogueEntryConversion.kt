package com.ruslan.growsseth.dialogues

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

object DialogueEntryConversion {
    const val KEY_CONTENT = "content"
    const val KEY_TEXT = "text"

    /**
     * returns: dialogue file with text entries replaced to keys, and dialogues lang object with the extracted text in the keys
     */
    fun extractKeysFromDialogueFile(root: JsonObject): Pair<JsonObject, JsonObject> {
        TODO("dialogues file")
    }

    fun transformOldDialogueFile(root: JsonObject): JsonObject {
        // old format changes dialogue lines mainly, so everything else is the same
        // object containing shared + events, each field is an array containing a list of dialogue entries
        // dialogue entries outside of shared can have only an id, referencing a shared dialogue,
        // and can also be a string (direct text) instead of an object

        return root.mapValues { (key, dialogues) ->
            dialogues.jsonArray.map(::transformOldDialogueEntry).let(::JsonArray)
        }.let(::JsonObject)
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