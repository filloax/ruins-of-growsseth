package com.ruslan.growsseth.worldgen.worldpreset

import com.filloax.fxlib.api.codec.forNullableGetter
import com.filloax.fxlib.api.codec.FxCodecs
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.utils.serverLang
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max
import kotlin.math.min

@Serializable
data class LocationData(
    @Serializable(with = LocationNameSerializer::class)
    val name: String,
    val x: Double,
    val y: Double,
    val z: Double,
    @Serializable(with = DoubleAsStringSerializer::class)
    val x1: Double? = null,
    @Serializable(with = DoubleAsStringSerializer::class)
    val y1: Double? = null,
    @Serializable(with = DoubleAsStringSerializer::class)
    val z1: Double? = null,
    @Serializable(with = DoubleAsStringSerializer::class)
    val x2: Double? = null,
    @Serializable(with = DoubleAsStringSerializer::class)
    val y2: Double? = null,
    @Serializable(with = DoubleAsStringSerializer::class)
    val z2: Double? = null,
    val hidden: Boolean = false,
) {
    init {
        if (x1 != null || x2 != null || y1 != null || y2 != null) {
            assert(x1 != null && x2 != null && y1 != null && y2 != null) { "Border coordinates must be present (x1, x2, y1 and y2) if one of them is set" }
        }
    }

    @Transient
    val centerPos = Vec3(x, y, z)
    @Transient
    val boundingBox = x1?.let{
        val z1_ = z1 ?: Constants.MIN_HEIGHT.toDouble()
        val z2_ = z2 ?: Constants.MAX_HEIGHT.toDouble()
        AABB(
            min(x1, x2!!), min(y1!!, y2!!), min(z1_, z2_),
            max(x1, x2), max(y1, y2), max(z1_, z2_)
        )
    }

    constructor(
        name: String, centerPos: Vec3, boundingBox: AABB? = null,
        hidden: Boolean = false
    ): this(
        name,
        centerPos.x, centerPos.y, centerPos.z,
        boundingBox?.minX, boundingBox?.minY, boundingBox?.minZ,
        boundingBox?.maxX, boundingBox?.maxY, boundingBox?.maxZ,
        hidden
    )

    companion object {
        val CODEC: Codec<LocationData> = RecordCodecBuilder.create { builder -> builder.group(
                Codec.STRING.fieldOf("name").forGetter(LocationData::name),
                Vec3.CODEC.fieldOf("centerPos").forGetter(LocationData::centerPos),
                FxCodecs.AABB.optionalFieldOf("boundingBox").forNullableGetter(LocationData::boundingBox),
                Codec.BOOL.fieldOf("hidden").forGetter(LocationData::hidden),
        ).apply(builder) { name, centerPos, boundingBox, hidden ->
            LocationData(name, centerPos, boundingBox.getOrNull(), hidden)
        } }
    }

    object DoubleAsStringSerializer : KSerializer<Double?> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DoubleAsStringSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Double?) {
            value?.let{ encoder.encodeString(it.toString()) } ?: ""
        }

        override fun deserialize(decoder: Decoder): Double? {
            val value = decoder.decodeString()
            return if (value.isBlank()) {
                null
            } else {
                value.toDouble()
            }
        }
    }
}

class LocationNameSerializer : JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return if (isHardcodedName(element)) {
            getHardcodedName(element as JsonObject)
        } else {
            getLocalizedName(element)
        }
    }

    private fun isHardcodedName(element: JsonElement): Boolean {
        return when (element) {
            is JsonPrimitive ->   // localization key
                false
            is JsonObject ->
                true
            else ->
                throw SerializationException("Unrecognized places element $element, was supposed to be a string or JSON object")
        }
    }

    private fun getHardcodedName(element: JsonObject): JsonPrimitive {
        return element["text"]?.let {
            if (it is JsonPrimitive)
                JsonPrimitive(it.content)
            else
                throw SerializationException("Place name $it is hardcoded but was wrongly formatted, it should be a string")
        } ?: throw SerializationException("Place name $element is a JSON object, but the 'text' key could not be found")
    }

    private fun getLocalizedName(element: JsonElement): JsonPrimitive {
        val key = (element as JsonPrimitive).content.trim()
        val prefixedKey = prefixKey(key)
        val localizedName = serverLang().getOrDefault(prefixedKey)
        return JsonPrimitive(localizedName)
    }

    private fun prefixKey(key: String): String {
        assertValidLangKey(key)
        val newKey = "${Constants.LANG_PLACES_PREFIX}.$key"
        return newKey
    }

    private fun assertValidLangKey(key: String) {
        val pattern = Regex("^(\\w+\\.)*\\w+$")
        if (!pattern.containsMatchIn(key)) {
            throw IllegalArgumentException("Wrongly formatted lang key $key")
        }
    }
}