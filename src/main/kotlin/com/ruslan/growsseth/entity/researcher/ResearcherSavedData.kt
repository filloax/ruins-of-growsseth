package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.codec.constructorWithOptionals
import com.filloax.fxlib.codec.forNullableGetter
import com.filloax.fxlib.codec.mutableMapCodec
import com.filloax.fxlib.nbt.getCompoundOrNull
import com.filloax.fxlib.nbt.loadField
import com.filloax.fxlib.nbt.saveField
import com.filloax.fxlib.savedata.FxSavedData
import com.filloax.fxlib.savedata.FxSavedData.Companion.loadData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import java.util.*
import javax.xml.crypto.Data

/**
 * Save researcher data in the level to allow it to persist even if he gets respawned.
 * Mainly meant for the livestream ver, can be tweaked to work in the commmunity ver too.
 */
class ResearcherSavedData private constructor (
    var data: CompoundTag,
    var name: Component?,
    var donkeyUuid: UUID?,
) : FxSavedData<ResearcherSavedData>(CODEC) {
    private lateinit var masterData: SavedData

    companion object {
        val CODEC: Codec<ResearcherSavedData> = RecordCodecBuilder.create { builder -> builder.group(
            CompoundTag.CODEC.fieldOf("data").forGetter(ResearcherSavedData::data),
            ComponentSerialization.CODEC.optionalFieldOf("name").forNullableGetter(ResearcherSavedData::name),
            UUIDUtil.STRING_CODEC.optionalFieldOf("donkeyUuid").forNullableGetter(ResearcherSavedData::donkeyUuid),
        ).apply(builder, constructorWithOptionals(ResearcherSavedData::class)::newInstance) }

        // Crashes with non-string key codec
        val CONTAINER_CODEC: Codec<DataMap> = mutableMapCodec(Codec.STRING, CODEC)
            .xmap({ map -> DataMap(map.mapKeys { it.key.toInt() }) }, { obj -> obj.items.mapKeys { it.key.toString() }.toMutableMap() })

        private val DEF = define("researcher_data", {DataMap()}, CONTAINER_CODEC)

        fun getContainer(server: MinecraftServer): DataMap {
            return server.loadData(DEF)
        }

        @JvmStatic
        fun getAll(server: MinecraftServer): MutableMap<Int, ResearcherSavedData> {
            return getContainer(server).items
        }

        @JvmStatic
        fun get(server: MinecraftServer, id: Int): ResearcherSavedData? {
            return getAll(server)[id]
        }

        @JvmStatic
        fun getOrCreate(server: MinecraftServer, id: Int): ResearcherSavedData {
            return getAll(server)[id] ?: createNew(server, id)
        }

        @JvmStatic
        fun getFreeId(server: MinecraftServer): Int {
            return (getContainer(server).items.keys.maxOrNull() ?: -1) + 1
        }

        @JvmStatic
        fun createNew(server: MinecraftServer, id: Int): ResearcherSavedData {
            val container = getContainer(server)
            val new = ResearcherSavedData(CompoundTag(), Component.empty(), null).initParent(container)
            container.items[id] = new
            return new
        }
    }

    fun initParent(masterData: SavedData): ResearcherSavedData {
        this.masterData = masterData
        return this
    }

    override fun setDirty() {
        super.setDirty()
        masterData.setDirty()
    }

    override fun setDirty(dirty: Boolean) {
        super.setDirty(dirty)
        if (dirty) {
            masterData.isDirty = dirty
        }
    }

    class DataMap(initialItems: Map<Int, ResearcherSavedData> = mapOf()) : FxSavedData<DataMap>(CONTAINER_CODEC) {
        val items = mutableMapOf<Int, ResearcherSavedData>().also {
            it.putAll(initialItems)
        }
    }
}