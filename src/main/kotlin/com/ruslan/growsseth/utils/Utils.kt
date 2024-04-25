package com.ruslan.growsseth.utils

import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.trading.MerchantOffer


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