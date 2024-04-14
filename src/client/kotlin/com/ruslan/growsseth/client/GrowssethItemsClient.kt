package com.ruslan.growsseth.client

import com.ruslan.growsseth.item.GrowssethItems
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

object GrowssethItemsClient {
    fun init() {
        ItemProperties.register(GrowssethItems.RESEARCHER_HORN, ResourceLocation("tooting")) { itemStack: ItemStack, _, livingEntity: LivingEntity?, _ ->
            if (livingEntity != null && livingEntity.isUsingItem && livingEntity.useItem == itemStack) 1.0f else 0.0f
        }
    }
}