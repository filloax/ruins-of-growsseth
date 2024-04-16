package com.ruslan.growsseth.advancements

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.structure.RemoteStructures
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.structure.Structure

object GrowssethAdvancements {
    @JvmStatic
    val all = HashSet<ResourceLocation>()

    @JvmField
    val TABS_WITH_SINGLE_BACKGROUND = mutableSetOf<String>(
        "growsseth",
    )

    // Defined via data or datagen

    val ROOT = make("root")
    val START = make("start")

    val FOR_STRUCTURES: Map<ResourceKey<Structure>, ResourceLocation> by lazy {
        GrowssethStructures.all.associateWith { StructureAdvancements.getStructureAdvancementId(it) }
    }

    fun make(name: String, folder: String = "growsseth"): ResourceLocation {
        val res = resLoc("$folder/$name")
        all.add(res)
        return res
    }

    object Callbacks {
        fun onServerTick(server: MinecraftServer) {
            // Check every 2 seconds for lag prevention
            if (server.tickCount % 40 == 0) {
                val spawningTent = RemoteStructures.STRUCTS_TO_SPAWN_BY_ID.values.any { it.structure == GrowssethStructures.RESEARCHER_TENT.location() }
                val advancement = server.advancements.get(START)
                if (advancement == null) {
                    RuinsOfGrowsseth.LOGGER.warn("No $START advancement!")
                    return
                }
                if (spawningTent)
                    server.playerList.players.forEach { player ->
                        if (!player.advancements.getOrStartProgress(advancement).isDone)
                            player.advancements.award(advancement, "requirement")
                    }
            }
        }
    }
}