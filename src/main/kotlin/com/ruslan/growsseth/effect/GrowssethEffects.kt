package com.ruslan.growsseth.effect

import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.Mob
import kotlin.reflect.KProperty

object GrowssethEffects {
    val all = mutableMapOf<ResourceLocation, RegistryHolderDelegate<MobEffect>>()

    val FIGHTING_SPIRIT by make("fighting_spirit", FightingSpiritEffect(MobEffectCategory.BENEFICIAL, 15630397))
    val JUSTICE         by make("justice", JusticeEffect(MobEffectCategory.BENEFICIAL, 15976537))
    val SERENITY        by make("serenity", SerenityEffect(MobEffectCategory.BENEFICIAL, 10013166))

    private fun make(name: String, effect: MobEffect): RegistryHolderDelegate< MobEffect> {
        val resourceLocation = resLoc(name)
        if (all.containsKey(resourceLocation))
            throw IllegalArgumentException("Effect $name already registered!")
        return RegistryHolderDelegate<MobEffect>(name, effect).also {
            all[resourceLocation] = it
        }
    }

    fun registerEffects(registrator: (ResourceLocation, MobEffect) -> Holder<MobEffect>) {
        all.forEach{
            it.value.initHolder(registrator(it.key, it.value.mobEffect))
        }
    }

    class RegistryHolderDelegate<T>(
        val name: String,
        val mobEffect: MobEffect,
    ) {
        var holder: Holder<T>? = null

        fun initHolder(holder: Holder<T>) {
            this.holder = holder
        }

        operator fun getValue(owner: Any, property: KProperty<*>): Holder<T> {
            return holder ?: throw IllegalStateException("Not initialized holder yet for $name")
        }
    }
}