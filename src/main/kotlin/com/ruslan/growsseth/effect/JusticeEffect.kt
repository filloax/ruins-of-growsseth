package com.ruslan.growsseth.effect

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity

class JusticeEffect(category: MobEffectCategory, color: Int) : MobEffect(category, color) {
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        val i = 10 shr amplifier        // like regeneration but starts at 1 hearth per second
        return if (i > 0)
            duration % i == 0
        else
            true
    }

    override fun applyEffectTick(livingEntity: LivingEntity, amplifier: Int): Boolean {
        if (livingEntity.health < livingEntity.maxHealth)
            livingEntity.heal(1.0f)
        return true
    }
}