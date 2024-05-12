package com.ruslan.growsseth.item

import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tier
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments

class ResearcherDaggerItem(tier: Tier, properties: Properties) : SwordItem(tier, properties){
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("${stack.descriptionId}.description")
            .withStyle(
                Style.EMPTY
                .applyFormat(ChatFormatting.GOLD)
            ))
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced)
    }

    override fun canBeEnchantedWith(
        stack: ItemStack,
        enchantment: Enchantment,
        context: EnchantingContext
    ): Boolean {
        if (listOf(Enchantments.KNOCKBACK, Enchantments.SWEEPING_EDGE).contains(enchantment))
            return false
        if (enchantment == Enchantments.BREACH)
            return true
        return super.canBeEnchantedWith(stack, enchantment, context)
    }
}