package com.ruslan.growsseth.compat.data

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike

/**
 * Cannot directly have items from other mods
 * in loot pools without breaking them, so create
 * a tag for each item used in recipes automatically.
 */
class OptionalLootItemTags {
    private val registeredItems: MutableMap<TagKey<Item>, ResourceKey<Item>> = mutableMapOf()

    /**
     * Create a tag entry containing the related item, will also create the tag after.
     */
    fun optionalModLootTableItem(item: ItemLike): TagKey<Item> {
        // Deprecated by mojang without offering a replacement
        val holder = item.asItem().builtInRegistryHolder()
        val key = holder.key()

        return optionalModLootTableItem(key)
    }

    fun optionalModLootTableItem(key: ResourceKey<Item>): TagKey<Item> {
        val tag = tagFromKey(key.location())

        registeredItems[tag] = key

        return tag
    }

    fun optionalModLootTableItem(id: ResourceLocation): TagKey<Item> {
        return optionalModLootTableItem(ResourceKey.create(Registries.ITEM, id))
    }

    val itemTags: Map<TagKey<Item>, ResourceKey<Item>> get() {
        if (registeredItems.isEmpty()) {
            throw Exception("No optional loot table items registered, did things run in the correct order?")
        }

        return registeredItems.toMap()
    }

    private fun tagFromKey(id: ResourceLocation) = TagKey.create(Registries.ITEM, id.withPrefix("mod_compat_loot_table_items/"))
}