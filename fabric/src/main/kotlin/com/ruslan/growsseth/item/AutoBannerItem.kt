package com.ruslan.growsseth.item

import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.item.BannerPatternItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BannerPattern

// Marker interface, data can already be obtained from TagKey
interface ItemWithBannerPattern {
    fun getTag(): TagKey<BannerPattern>
}

class AutoBannerItem(
    val tagKey: TagKey<BannerPattern>,
    properties: Properties,
    private val keepName: Boolean = true,
) : BannerPatternItem(tagKey, properties.stacksTo(1 )), ItemWithBannerPattern {
    override fun getTag(): TagKey<BannerPattern> = tagKey

    override fun getName(itemStack: ItemStack): Component {
        return if (keepName) {
            return Items.FLOWER_BANNER_PATTERN.getName(itemStack)
        }  else {
            super.getName(itemStack)
        }
    }
}