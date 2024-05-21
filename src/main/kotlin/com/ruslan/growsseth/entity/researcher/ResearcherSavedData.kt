package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.codec.forNullableGetter
import com.filloax.fxlib.api.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.MinecraftServer
import kotlin.jvm.optionals.getOrNull

/**
 * Save researcher data in the level to allow it to persist even if he gets respawned.
 */
class ResearcherSavedData private constructor (
    var data: CompoundTag = CompoundTag(),
    var name: Component? = null,
    var isDead: Boolean = false,
) : FxSavedData<ResearcherSavedData>(CODEC) {

    companion object {
        val CODEC: Codec<ResearcherSavedData> = RecordCodecBuilder.create { builder -> builder.group(
            CompoundTag.CODEC.fieldOf("data").forGetter(ResearcherSavedData::data),
            ComponentSerialization.CODEC.optionalFieldOf("name").forNullableGetter(ResearcherSavedData::name),
            Codec.BOOL.fieldOf("isDead").forGetter(ResearcherSavedData::isDead)
        ).apply(builder) { data, name, isDead ->
            ResearcherSavedData(data, name.getOrNull(), isDead)
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