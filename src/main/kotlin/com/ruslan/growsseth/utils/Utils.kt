package com.ruslan.growsseth.utils

import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.resources.ResourceLocation


fun resLoc(str: String): ResourceLocation {
    return ResourceLocation(RuinsOfGrowsseth.MOD_ID, str)
}