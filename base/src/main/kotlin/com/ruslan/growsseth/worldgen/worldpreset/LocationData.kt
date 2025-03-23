package com.ruslan.growsseth.worldgen.worldpreset

import com.filloax.fxlib.api.codec.forNullableGetter
import com.filloax.fxlib.api.codec.FxCodecs
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.utils.serverLang
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max
import kotlin.math.min

@Serializable
/**
 * Name can be passed as string (will be treated as fxlib server-side lang),
 * or as object (see LocationName class)
 */
data class LocationData(
    @Serializable(with = LocationNameSerializer::class)
    val name: LocationName,
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

    companion object {
        // Drops the "isLocalizable" field as not needed for client anyways (should be processed in the meantime)
        val CODEC: Codec<LocationData> = RecordCodecBuilder.create { builder -> builder.group(
            Codec.STRING.fieldOf("name").forGetter{ it.name.text },
            Vec3.CODEC.fieldOf("centerPos").forGetter(LocationData::centerPos),
            FxCodecs.AABB.optionalFieldOf("boundingBox").forNullableGetter(LocationData::boundingBox),
            Codec.BOOL.fieldOf("hidden").forGetter(LocationData::hidden),
        ).apply(builder) { name, centerPos, boundingBox, hidden ->
            LocationData(name, centerPos, boundingBox.getOrNull(), hidden)
        } }
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
        LocationName(name),
        centerPos.x, centerPos.y, centerPos.z,
        boundingBox?.minX, boundingBox?.minY, boundingBox?.minZ,
        boundingBox?.maxX, boundingBox?.maxY, boundingBox?.maxZ,
        hidden
    )

    /**
     * Must run before sending packet to client, do not send the object with unlocalised names
     */
    fun processLocationName(): LocationData {
        return this.copy(name = LocationName(processLocationNameStr()))
    }

    private fun processLocationNameStr(): String {
        return if (name.isLocalizable) {
            val key = name.text.trim()
            val prefixedKey = prefixKey(key)
            return serverLang().getOrDefault(prefixedKey)
        } else {
            name.text
        }
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

@Serializable
data class LocationName(
    val text: String,
    val isLocalizable: Boolean = true,
)

class LocationNameSerializer : JsonTransformingSerializer<LocationName>(LocationName.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonPrimitive -> JsonObject(mapOf("text" to element))
            else -> element
        }
    }
}