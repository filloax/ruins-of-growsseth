package com.ruslan.growsseth.worldgen

import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.presets.WorldPreset

object GrowssethModWorldPresets {
    @JvmField
    val GROWSSETH = register("growsseth")

    private fun register(name: String): ResourceKey<WorldPreset> {
        return ResourceKey.create(Registries.WORLD_PRESET, resLoc(name))
    }

    fun bootstrap(ctx: BootstrapContext<WorldPreset>) {
        ctx.register(GROWSSETH, GrowssethWorldPreset.build(ctx))
    }
}