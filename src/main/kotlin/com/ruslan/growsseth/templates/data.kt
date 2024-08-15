package com.ruslan.growsseth.templates

import com.filloax.fxlib.api.codec.decodeJson
import com.google.gson.Gson
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.world.entity.Entity

private val gson = Gson().newBuilder().setLenient().create()

interface TemplateData {
    val entries: List<TemplateEntry>

    fun entriesComponents() = entries.map {
        if (it.type == TemplateEntryType.STRING)
            Component.literal(it.content)
        else
            ComponentSerialization.CODEC
                .decodeJson(gson.fromJson(it.content, com.google.gson.JsonElement::class.java)).result()
                .orElseThrow { Exception("Couldn't parse component") }.first
    }

    companion object {
        fun singleEntry(str: String) = TemplateEntry(TemplateEntryType.STRING, str)
    }
}

@Serializable
data class SimpleTemplateData(
    @Serializable(with = TemplateEntriesSerializer::class)
    override val entries: List<TemplateEntry>
) : TemplateData


@Serializable(with = TemplateKindSerializer::class)
class TemplateKind<T : TemplateData> private constructor(val path: String, val serializer: KSerializer<T>) {
    companion object {
        private val instances = mutableMapOf<String, TemplateKind<out TemplateData>>()
        
        val BOOK = register(TemplateKind("book", BookData.serializer()))
        val SIGN = register(TemplateKind("sign", SignData.serializer()))

        val all: Map<String, TemplateKind<out TemplateData>> get() = instances

        private fun <T:TemplateData> register(x: TemplateKind<T>) = x.also {
            instances[it.path] = it
        }

        fun fromPath(path: String): TemplateKind<out TemplateData> {
            return instances[path] ?: throw IllegalArgumentException("Unknown path: $path")
        }
    }
}

object TemplateKindSerializer : KSerializer<TemplateKind<out TemplateData>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TemplateKind", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TemplateKind<out TemplateData>) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(decoder: Decoder): TemplateKind<out TemplateData> {
        val path = decoder.decodeString()
        return TemplateKind.fromPath(path)
    }
}


@Serializable
data class TemplateEntry(
    val type: TemplateEntryType,
    val content: String,
)

enum class TemplateEntryType  {
    @SerialName("component")
    COMPONENT,
    @SerialName("string")
    STRING,
}

private class TemplateEntriesSerializer : JsonTransformingSerializer<List<TemplateEntry>>(
    ListSerializer(TemplateEntry.serializer())
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


@Serializable
data class BookData(
    @Serializable(with = TemplateEntriesSerializer::class)
    val pages: List<TemplateEntry>,     // for backward compatibility, use entries otherwise
    val name: String? = null,
    val author: String? = null,
    val writable: Boolean = false,
) : TemplateData {
    @Transient
    override val entries = pages

    @Transient
    val pagesComponents = entriesComponents()

    fun withAuthor(author: String) = copy(author = author)
    fun withAuthor(entity: Entity) = withAuthor(entity.name.string)

    companion object {
        fun pageEntry(str: String) = TemplateData.singleEntry(str)
    }
}


@Serializable
data class SignData(
    @Serializable(with = TemplateEntriesSerializer::class)
    override val entries: List<TemplateEntry>,
    val color: String? = "black",
    val glowing: Boolean? = false,
    // signs should be waxed ingame
) : TemplateData
