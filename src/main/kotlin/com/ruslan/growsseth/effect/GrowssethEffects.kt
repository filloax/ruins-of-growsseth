package com.ruslan.growsseth.effect

import com.filloax.fxlib.registration.RegistryHolderDelegate
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory

object GrowssethEffects {
    val all = mutableMapOf<ResourceLocation, RegistryHolderDelegate<MobEffect>>()

    val FIGHTING_SPIRIT by make("fighting_spirit", FightingSpiritEffect(MobEffectCategory.BENEFICIAL, 15630397))
    val JUSTICE         by make("justice", JusticeEffect(MobEffectCategory.BENEFICIAL, 15976537))
    val SERENITY        by make("serenity", SerenityEffect(MobEffectCategory.BENEFICIAL, 10013166))

    private fun make(name: String, effect: MobEffect) = RegistryHolderDelegate(resLoc(name), effect).apply {
        if (all.containsKey(id))
            throw IllegalArgumentException("Effect $name already registered!")
        all[id] = this
    }

    fun registerEffects(registrator: (ResourceLocation, MobEffect) -> Holder<MobEffect>) {
        all.values.forEach{
            it.initHolder(registrator(it.id, it.value))
        }
    }
}