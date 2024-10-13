package com.ruslan.growsseth.effect

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory

// Damage reduction from all sources, implementation is in LivingEntity mixin
class FightingSpiritEffect(category: MobEffectCategory, color: Int) : MobEffect(category, color) {
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean { return true }
}