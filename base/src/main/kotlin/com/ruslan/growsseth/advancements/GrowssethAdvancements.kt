package com.ruslan.growsseth.advancements

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.structure.RemoteStructures
import com.ruslan.growsseth.utils.isNull
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.structure.Structure

object GrowssethAdvancements {
    @JvmStatic
    val all = HashSet<Identifier>()

    @JvmField
    val TABS_WITH_SINGLE_BACKGROUND = mutableSetOf<String>(
        "growsseth",
    )

    // Defined via data or datagen

    val ROOT = make("root")
    val START = make("start")

    val FOR_STRUCTURES: Map<ResourceKey<Structure>, Identifier> by lazy {
        GrowssethStructures.all.associateWith { StructureAdvancements.getStructureAdvancementId(it) }
    }

    fun make(name: String, folder: String = "growsseth"): Identifier {
        val res = resLoc("$folder/$name")
        all.add(res)
        return res
    }

    object Callbacks {
        fun onServerTick(server: MinecraftServer) {
            // Check every 2 seconds for lag prevention
            if (server.tickCount % 40 == 0) {
                val spawningTent = RemoteStructures.STRUCTS_TO_SPAWN_BY_ID.values.any { it.structure.equals(GrowssethStructures.RESEARCHER_TENT.identifier()) }
                val advancement = server.advancements.get(START)
                if (isNull(advancement)) {
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