package com.ruslan.growsseth.effect

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory

// Only 1 damage received from all sources (except /kill and void)
class SerenityEffect(category: MobEffectCategory, color: Int) : MobEffect(category, color) {
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean { return true }
}