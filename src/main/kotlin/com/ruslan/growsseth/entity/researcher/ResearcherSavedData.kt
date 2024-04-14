package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.nbt.getCompoundOrNull
import com.filloax.fxlib.nbt.loadField
import com.filloax.fxlib.nbt.saveField
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

/**
 * Save researcher data in the level to allow it to persist even if he gets respawned.
 * Mainly meant for the livestream ver, can be tweaked to work in the commmunity ver too.
 */
class ResearcherSavedData private constructor (
    var data: CompoundTag,
    var name: Component?,
    var donkeyUuid: UUID?,
    private var masterData: SavedData,
) : SavedData() {
    companion object {
        fun getContainer(server: MinecraftServer): DataMap {
            return server.overworld().dataStorage.computeIfAbsent(Factory({
                RuinsOfGrowsseth.LOGGER.info("Creating new researcher data...")
                DataMap()
            }, ::loadMap, DataFixTypes.STRUCTURE ), "researcher_data")
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
            return getContainer(server).items.keys.max() + 1
        }

        @JvmStatic
        fun createNew(server: MinecraftServer, id: Int): ResearcherSavedData {
            val container = getContainer(server)
            val new = ResearcherSavedData(CompoundTag(), Component.empty(), null, container)
            container.items[id] = new
            return new
        }

        private fun load(compoundTag: CompoundTag, parent: SavedData): ResearcherSavedData {
            return ResearcherSavedData(
                compoundTag.getCompoundOrNull("data") ?:
                     throw IllegalStateException("Researcher world data doesn't have entity data set!")
                ,
                compoundTag.loadField("name", ComponentSerialization.CODEC),
                compoundTag.loadField("donkeyId", UUIDUtil.STRING_CODEC),
                parent,
            )
        }

        private fun loadMap(compoundTag: CompoundTag): DataMap {
            val out = DataMap()
            val mapTag = compoundTag.getCompound("researcherData")
            out.items.putAll(mapTag.allKeys.associate { id -> id.toInt() to load(mapTag.getCompound(id), out) })
            return out
        }
    }

    override fun save(compoundTag: CompoundTag): CompoundTag {
        compoundTag.put("data", data)
        compoundTag.saveField("name", ComponentSerialization.CODEC, this::name)
        compoundTag.saveField("donkeyId", UUIDUtil.STRING_CODEC, this::donkeyUuid)
        return compoundTag
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

    class DataMap : SavedData() {
        val items = mutableMapOf<Int, ResearcherSavedData>()

        override fun save(compoundTag: CompoundTag): CompoundTag {
            compoundTag.put("researcherData", CompoundTag().also {
                items.forEach { (id, data) ->
                    it.put(id.toString(), data.save(CompoundTag()))
                }
            })
            return compoundTag
        }
    }
}