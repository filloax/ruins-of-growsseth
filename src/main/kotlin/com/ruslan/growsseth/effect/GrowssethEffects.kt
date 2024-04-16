package com.ruslan.growsseth.effect

import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory

object GrowssethEffects {
    val all = mutableMapOf<ResourceLocation, MobEffect>()

    val FIGHTING_SPIRIT: MobEffect = make("fighting_spirit", FightingSpiritEffect(MobEffectCategory.BENEFICIAL, 15630397))
    val JUSTICE: MobEffect = make("justice", JusticeEffect(MobEffectCategory.BENEFICIAL, 15976537))
    val SERENITY: MobEffect = make("serenity", SerenityEffect(MobEffectCategory.BENEFICIAL, 10013166))

    private fun make(hashName: String, effect: MobEffect): MobEffect {
        val resourceLocation = resLoc(hashName)
        if (all.containsKey(resourceLocation))
            throw IllegalArgumentException("Effect $hashName already registered!")
        all[resourceLocation] = effect
        return effect
    }

    fun registerEffects(registrator: (ResourceLocation, MobEffect) -> Unit) {
        all.forEach{
            registrator(it.key, it.value)
        }
    }
}