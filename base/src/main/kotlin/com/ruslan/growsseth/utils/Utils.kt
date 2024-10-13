package com.ruslan.growsseth.utils

import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece
import net.minecraft.world.level.levelgen.structure.StructurePiece
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


fun resLoc(str: String): ResourceLocation {
    return ResourceLocation.fromNamespaceAndPath(RuinsOfGrowsseth.MOD_ID, str)
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

fun loadPropertiesFile(fileName: String): Map<String, String> {
    val properties = Properties()
    val propertiesMap = mutableMapOf<String, String>()

    val classLoader = Thread.currentThread().contextClassLoader
    val inputStream = classLoader.getResourceAsStream(fileName)
        ?: throw IllegalArgumentException("Properties file not found: $fileName")

    inputStream.use {
        properties.load(it)
    }

    for (key in properties.stringPropertyNames()) {
        propertiesMap[key] = properties.getProperty(key)
    }

    return propertiesMap
}

/**
 * Workaround for the IDE-only issue of marking == / != null as an error
 * when it isn't
 *
 * DISABLED: cannot do this with 'this' (and so extension methods) for now
 * in current version of contracts
 */
//@OptIn(ExperimentalContracts::class)
//fun Any?.isNull(): Boolean {
//    contract {
//        returns(true) implies (this == null)
//        returns(false) implies (this != null)
//    }
//    return this == null
//}

/**
 * Workaround for the IDE-only issue of marking == / != null as an error
 * when it isn't
 */
@OptIn(ExperimentalContracts::class)
fun isNull(x: Any?): Boolean {
    contract {
        returns(true) implies (x == null)
        returns(false) implies (x != null)
    }
    return x == null
}

/**
 * Workaround for the IDE-only issue of marking == / != null as an error
 * when it isn't
 */
@OptIn(ExperimentalContracts::class)
fun notNull(x: Any?): Boolean {
    contract {
        returns(true) implies (x != null)
        returns(false) implies (x == null)
    }
    return x != null
}