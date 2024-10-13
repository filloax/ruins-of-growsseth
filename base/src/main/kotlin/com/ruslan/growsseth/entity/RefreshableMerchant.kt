package com.ruslan.growsseth.entity

import net.minecraft.world.item.trading.Merchant

interface RefreshableMerchant : Merchant {
    fun refreshCurrentTrades()
}