package com.ruslan.growsseth.worldgen.worldpreset

import kotlinx.serialization.json.*

object LocationEntryConversion {
    const val KEY_NAME = "name"
    const val KEY_TEXT = "text"

    private val JSON = Json

    /**
     * returns: places file with text entries replaced to keys, and places lang object with the extracted text in the keys
     */
    fun extractKeysFromPlacesFile(root: JsonArray, langPrefix: String): Pair<JsonArray, JsonObject> {
        val entries: List<JsonElement> = JSON.decodeFromJsonElement(root)
        val langStrings = mutableMapOf<String, JsonElement>()
        val toKeys = entries.withIndex().map { (index, element) ->
            when (element) {
                is JsonObject -> extractKeysFromEntry(element, "$langPrefix.$index") { k, v -> langStrings[k] = JsonPrimitive(v) }
                else -> element
            }
        }.let(::JsonArray)

        return Pair(toKeys, JsonObject(langStrings))
    }

    private fun extractKeysFromEntry(placeEntry: JsonObject, prefix: String, keysInserter: (key: String, value: String) -> Unit): JsonElement {
        val content = placeEntry[KEY_NAME] ?: return placeEntry

        val newContent = when (content) {
            is JsonPrimitive -> content
            is JsonObject -> extractKeysFromContent(content, prefix, keysInserter)
            else -> return placeEntry
        }

        return JsonObject(
            mapOf(KEY_NAME to newContent) + placeEntry.minus(KEY_NAME)
        )
    }

    private fun extractKeysFromContent(content: JsonObject, key: String, keysInserter: (key: String, value: String) -> Unit): JsonElement {
        return if (content.containsKey(KEY_TEXT)) {
            keysInserter(key.split(".").last(), content[KEY_TEXT]!!.jsonPrimitive.content)  // removing prefix from lang id
            JsonPrimitive(key)
        } else { content }
    }
}