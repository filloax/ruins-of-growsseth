package com.ruslan.growsseth.structure

import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.advancements.criterion.JigsawPiecePredicate
import com.ruslan.growsseth.attachments.growssethAttachment
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.Structure
import java.time.LocalDateTime

/**
 * Track last time of visit for each mod structure,
 * also includes village houses associated to a structure
 * in [com.ruslan.growsseth.structure.GrowssethStructures.VILLAGE_HOUSE_STRUCTURES]
 */
object StructureVisitTracker {
    private const val UPDATE_PERIOD_TICKS = 20 // every second

    private val structureTags by lazy { GrowssethTags.StructTags.ALL.toList() }

    // Use our jigsaw predicates to simplify code on this class
    private val villageHousePredicates by lazy {
        GrowssethStructures.VILLAGE_HOUSE_STRUCTURES
            .mapKeys { (placeholderKey, _) -> GrowssethStructures.info[placeholderKey]!!.tag }
            .mapValues { (_, buildings) ->
                buildings.entries.map { (villageKey, pieceList) ->
                    JigsawPiecePredicate(villageKey, pieceList)
                }
            }
    }

    fun onServerPlayerTick(player: ServerPlayer) {
        if (player.tickCount % UPDATE_PERIOD_TICKS != 0) return

        val level = player.level()
        val structureManager = level.structureManager()

        for (tag in structureTags) {
            if (structureManager.getStructureWithPieceAt(player.onPos, tag).isValid) {
                player.lastStructureEnterTimes[tag] = LocalDateTime.now()
            }
        }

        for ((tag, predicates) in villageHousePredicates) {
            for (predicate in predicates) {
                if (predicate.matches(level, player.x, player.y, player.z)) {
                    player.lastStructureEnterTimes[tag] = LocalDateTime.now()
                    return
                }
            }
        }
    }

    private val ServerPlayer.lastStructureEnterTimes: MutableMap<TagKey<Structure>, LocalDateTime>
        get() = this.growssethAttachment().lastStructureEnterTimes

    fun getLastStructureEnterTime(player: ServerPlayer, tag: TagKey<Structure>): LocalDateTime? {
        return player.lastStructureEnterTimes[tag]
    }
}