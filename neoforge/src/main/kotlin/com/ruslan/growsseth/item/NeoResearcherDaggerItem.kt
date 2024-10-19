package com.ruslan.growsseth.item

import net.minecraft.core.Holder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.neoforged.neoforge.common.extensions.IItemExtension

class NeoResearcherDaggerItem : IItemExtension, AbstractResearcherDaggerItem() {
    override fun isBookEnchantable(stack: ItemStack, book: ItemStack): Boolean {
        // This could block enchants altogether if they contain the wrong enchantment?
        // for now always allow, better than the opposite, check if better way to do this later
        return super<IItemExtension>.isBookEnchantable(stack, book)
    }

    override fun isPrimaryItemFor(stack: ItemStack, enchantment: Holder<Enchantment>): Boolean {
        return this.allowEnchantment(stack, enchantment)
            .orElse(super<IItemExtension>.isPrimaryItemFor(stack, enchantment))
    }

    override fun isRepairable(p0: ItemStack): Boolean {
        return true
    }
}