package com.ruslan.growsseth.templates

import com.filloax.fxlib.api.codec.decodeJson
import com.filloax.fxlib.api.json.ComponentSerializer
import com.google.gson.Gson
import com.sun.jna.platform.unix.solaris.LibKstat.KstatNamed.UNION.STR
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.world.entity.Entity

private val gson = Gson().newBuilder().setLenient().create()

@Serializable
data class BookData(
    @Serializable(with = BookPagesSerializer::class)
    val pages: List<PageEntry>,
    val name: String? = null,
    val author: String? = null,
    val writable: Boolean = false,
) {
    @Transient
    val pagesComponents = pages.map { if (it.type == PageEntryType.STRING)
        Component.literal(it.content)
    else
        ComponentSerialization.CODEC
            .decodeJson(gson.fromJson(it.content, com.google.gson.JsonElement::class.java)).result()
            .orElseThrow { Exception("Couldn't parse book page component") }.first
    }

    fun withAuthor(author: String) = copy(author = author)
    fun withAuthor(entity: Entity) = withAuthor(entity.name.string)

    companion object {
        fun pageEntry(str: String) = PageEntry(PageEntryType.STRING, str)
    }

    @Serializable
    data class PageEntry(
        val type: PageEntryType,
        val content: String,
    )

    enum class PageEntryType  {
        @SerialName("component")
        COMPONENT,
        @SerialName("string")
        STRING,
    }

    private class BookPagesSerializer : JsonTransformingSerializer<List<PageEntry>>(
        ListSerializer(PageEntry.serializer())
    ) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            return when (element) {
                is JsonArray -> JsonArray(element.map(::transformDeserializeElement))
                else -> throw SerializationException("Unrecognized array $element")
            }
        }

        private fun transformDeserializeElement(element: JsonElement): JsonElement {
            return when (element) {
                is JsonPrimitive -> {
                    if (element.isString) {
                        JsonObject(mapOf(
                            "type" to JsonPrimitive("string"),
                            "content" to element
                        ))
                    } else {
                        throw SerializationException("Unexpected non-string JsonPrimitive: $element")
                    }
                }
                is JsonObject -> stringifyPageContent(element)
                else -> element // Will error, but the error message is clearer if the serializer fails instead of us
            }
        }

        // Stringify content so it can be loaded by the component parser
        private fun stringifyPageContent(obj: JsonObject): JsonObject {
            val content = obj["content"] ?: throw SerializationException("Page doesn't have content! Is $obj")
            val stringifiedContent = when(content) {
                is JsonObject, is JsonArray -> JsonPrimitive(content.toString())
                else -> content
            }
            return buildJsonObject {
                obj.keys.minus("content").forEach { put(it, obj[it]!!) }
                put("content", stringifiedContent)
            }
        }
    }
}