package com.ruslan.growsseth.worldgen.worldpreset

import com.filloax.fxlib.codec.FxCodecs
import com.filloax.fxlib.codec.constructorWithOptionals
import com.filloax.fxlib.codec.forNullableGetter
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.Constants
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.fabricmc.loader.impl.lib.sat4j.core.Vec
import net.minecraft.core.BlockPos
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max
import kotlin.math.min

@Serializable
data class LocationData(
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