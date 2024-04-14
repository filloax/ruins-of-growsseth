package com.ruslan.growsseth.item

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item

object PotPatternRegistry {
    private val registry = mutableMapOf<Item, ResourceKey<String>>()

    @JvmStatic
    fun register(item: Item, patternPath: ResourceLocation) {
        if (registry.containsKey(item)) {
            throw IllegalArgumentException("Pot pattern registry already contains entry for $item: ${registry[item]}")
        }
        Registry.register(BuiltInRegistries.DECORATED_POT_PATTERNS, patternPath, patternPath.path)

        registry[item] = ResourceKey.create(Registries.DECORATED_POT_PATTERNS, patternPath)
    }

    @JvmStatic
    fun getForItem(item: Item): ResourceKey<String>? {
        return registry[item]
    }
}