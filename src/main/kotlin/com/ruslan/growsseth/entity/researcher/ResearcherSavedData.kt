package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.codec.forNullableGetter
import com.filloax.fxlib.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.MinecraftServer
import kotlin.jvm.optionals.getOrNull

/**
 * Save researcher data in the level to allow it to persist even if he gets respawned.
 * Mainly meant for the livestream ver, can be tweaked to work in the commmunity ver too.
 */
class ResearcherSavedData private constructor (
    var data: CompoundTag = CompoundTag(),
    var name: Component? = null,
) : FxSavedData<ResearcherSavedData>(CODEC) {

    companion object {
        val CODEC: Codec<ResearcherSavedData> = RecordCodecBuilder.create { builder -> builder.group(
            CompoundTag.CODEC.fieldOf("data").forGetter(ResearcherSavedData::data),
            ComponentSerialization.CODEC.optionalFieldOf("name").forNullableGetter(ResearcherSavedData::name),
        ).apply(builder) { data, name ->
            ResearcherSavedData(data, name.getOrNull())
        } }

        private val DEF = define("researcher_data", ::ResearcherSavedData, CODEC)

        @JvmStatic
        fun getPersistent(server: MinecraftServer): ResearcherSavedData {
            return server.loadData(DEF)
        }

        @JvmStatic
        fun create(): ResearcherSavedData {
            return DEF.provider()
        }
    }
}