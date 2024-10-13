package com.ruslan.growsseth.worldgen

import com.mojang.serialization.Codec
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.biome.BiomeSource
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList
import net.minecraft.world.level.biome.TheEndBiomeSource

object GrowssethModBiomeSources {
    val allSources = mutableMapOf<ResourceLocation, Codec<out BiomeSource>>()

    @JvmField
    val GROWSSETH_OVERWORLD_SETTINGS = multiNoiseSettings("growsseth_overworld")

    fun multiNoiseSettings(key: String): ResourceKey<MultiNoiseBiomeSourceParameterList> {
        return ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, resLoc(key))
    }
}