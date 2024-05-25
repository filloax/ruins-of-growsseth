package com.ruslan.growsseth.utils

import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece
import net.minecraft.world.level.levelgen.structure.StructurePiece
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement


fun resLoc(str: String): ResourceLocation {
    return ResourceLocation(RuinsOfGrowsseth.MOD_ID, str)
}

fun MerchantOffer.contentEquals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MerchantOffer) return false

    if (baseCostA != other.baseCostA) return false
    if (costB != other.costB) return false
    if (result != other.result) return false
    if (uses != other.uses) return false
    if (maxUses != other.maxUses) return false
    if (rewardExp != other.rewardExp) return false
    if (specialPriceDiff != other.specialPriceDiff) return false
    if (demand != other.demand) return false
    if (priceMultiplier != other.priceMultiplier) return false
    if (xp != other.xp) return false

    return true
}

fun StructurePiece.matchesJigsaw(pieceIds: Collection<ResourceLocation>): Boolean {
    if (this is PoolElementStructurePiece) {
        return this.element.matches(pieceIds)
    }
    return false
}

fun StructurePoolElement.matches(pieceIds: Collection<ResourceLocation>): Boolean {
    return when (this) {
        // won't work with runtime elements (aka saved without ids)
        is SinglePoolElement -> this.template.left().map{ pieceIds.contains(it) }.orElse(false)
        is ListPoolElement -> this.elements.any { it.matches(pieceIds) }
        else -> false
    }
}

fun StructurePoolElement.getTemplateIds(): Collection<ResourceLocation> {
    return when (this) {
        // won't work with runtime elements (aka saved without ids)
        is SinglePoolElement -> this.template.left().map { listOf(it) }.orElse(listOf())
        is ListPoolElement -> this.elements.flatMap { it.getTemplateIds() }
        else -> listOf()
    }
}