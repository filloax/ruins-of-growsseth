package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.codec.forNullableGetter
import com.filloax.fxlib.api.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ResearcherConfig
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.MinecraftServer
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

/**
 * Save researcher data in the server to allow it to persist even if he gets respawned.
 * Supposed to be singular instance per server, to properly track the last time it was updated
 * (such that different entities can know another entity updated the data after them and update themselves accordingly).
 */
class ResearcherSavedData private constructor (
    var data: CompoundTag = CompoundTag(),
    var name: Component? = null,
    var isDead: Boolean = false,
) : FxSavedData<ResearcherSavedData>(CODEC) {
    var lastChangeTimestamp: LocalDateTime = LocalDateTime.now()
        private set

    companion object {
        val CODEC: Codec<ResearcherSavedData> = RecordCodecBuilder.create { builder -> builder.group(
            CompoundTag.CODEC.fieldOf("data").forGetter(ResearcherSavedData::data),
            ComponentSerialization.CODEC.optionalFieldOf("name").forNullableGetter(ResearcherSavedData::name),
            Codec.BOOL.fieldOf("isDead").forGetter(ResearcherSavedData::isDead)
        ).apply(builder) { data, name, isDead ->
            ResearcherSavedData(data, name.getOrNull(), isDead)
        } }

        private val DEF = define("researcher_data", ::ResearcherSavedData, CODEC)

        private var instance: ResearcherSavedData? = null

        @JvmStatic
        fun getPersistent(server: MinecraftServer): ResearcherSavedData {
            return instance ?: server.loadData(DEF).also {
                RuinsOfGrowsseth.LOGGER.info("Create researcher saved data!")
                instance = it
            }
        }

        @JvmStatic
        fun create(): ResearcherSavedData {
            if (ResearcherConfig.singleResearcher) {
                // Ensure we don't get more than one instance
                throw IllegalStateException("Cannot create saved data when in single researcher mode!")
            }
            return DEF.provider()
        }
    }

    override fun setDirty() {
        super.setDirty()
        lastChangeTimestamp = LocalDateTime.now()
    }

    object Callbacks {
        fun onServerStopped(server: MinecraftServer) {
            instance = null
        }
    }
}