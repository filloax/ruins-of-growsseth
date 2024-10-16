package com.ruslan.growsseth.item

import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.fabricmc.fabric.api.item.v1.FabricItem
import net.minecraft.core.Holder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.enchantment.Enchantment

class FabricResearcherDaggerItem : FabricItem, AbstractResearcherDaggerItem() {
    // Manually implement FabricItem as intellij/gradle doesn't regonize it otherwise

    override fun canBeEnchantedWith(
        stack: ItemStack,
        enchantment: Holder<Enchantment>,
        context: EnchantingContext,
    ): Boolean {
        val allow = this.allowEnchantment(stack, enchantment)
        if (allow.isPresent) {
            return allow.orElseThrow()
        } else {
            return super.canBeEnchantedWith(stack, enchantment, context)
        }
    }
}