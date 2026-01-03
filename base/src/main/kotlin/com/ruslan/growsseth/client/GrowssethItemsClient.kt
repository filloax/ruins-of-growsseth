package com.ruslan.growsseth.client

import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.utils.notNull
//import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

object GrowssethItemsClient {
    fun init() {
        // todo: see if it it's not needed anymore, since horns are data driven since 1.21.3
//        ItemProperties.register(GrowssethItems.RESEARCHER_HORN, Identifier.parse("tooting")) { itemStack: ItemStack, _, livingEntity: LivingEntity?, _ ->
//            if (notNull(livingEntity) && livingEntity.isUsingItem && livingEntity.useItem == itemStack) 1.0f else 0.0f
//        }
    }
}