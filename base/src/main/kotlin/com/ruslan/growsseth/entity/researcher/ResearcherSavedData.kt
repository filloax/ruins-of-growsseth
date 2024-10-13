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
    data: CompoundTag = CompoundTag(),
    name: Component? = null,
    isDead: Boolean = false,
) : FxSavedData<ResearcherSavedData>(CODEC) {
    // Inner class is its own class for easier tracking of changes
    // for sync purposes; this is NOT efficient when setting these
    // fields often but that should not be happening
    private var currentData = InnerData(data, name, isDead)
    private var previousData: InnerData = currentData.copy()

    var data
        get() = currentData.data
        set(value) { currentData = currentData.copy(data = value) }
    var name
        get() = currentData.name
        set(value) { currentData = currentData.copy(name = value) }
    var isDead
        get() = currentData.isDead
        set(value) { currentData = currentData.copy(isDead = value) }

    var lastChangeTimestamp: LocalDateTime = LocalDateTime.now()
        private set

    companion object {
        private val CODEC: Codec<ResearcherSavedData> = RecordCodecBuilder.create { builder -> builder.group(
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
            if (com.ruslan.growsseth.config.ResearcherConfig.singleResearcher) {
                // Ensure we don't get more than one instance
                throw IllegalStateException("Cannot create saved data when in single researcher mode!")
            }
            return DEF.provider()
        }
    }

    override fun setDirty() {
        if (currentData != previousData) {
            super.setDirty()
            previousData = currentData.copy(data = currentData.data.copy(), name = currentData.name?.copy())
            lastChangeTimestamp = LocalDateTime.now()
        }
    }

    // Make inner data its own class so we can more easily check if data was changed before
    // updating last change time
    data class InnerData(val data: CompoundTag, val name: Component?, val isDead: Boolean) {
        override fun equals(other: Any?): Boolean {
            if (other !is InnerData) return false

            return name == other.name
                && isDead == other.isDead
                && data == other.data
        }

        override fun hashCode(): Int {
            var result = data.hashCode()
            result = 31 * result + (name?.hashCode() ?: 0)
            result = 31 * result + isDead.hashCode()
            return result
        }
    }

    object Callbacks {
        fun onServerStopped(server: MinecraftServer) {
            instance = null
        }
    }
}