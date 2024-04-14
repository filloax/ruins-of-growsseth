package com.ruslan.growsseth.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tier
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class ResearcherDaggerItem(tier: Tier, attackDamageModifier: Int, attackSpeedModifier: Float, properties: Properties ) :
    SwordItem(tier, attackDamageModifier, attackSpeedModifier, properties){
        override fun appendHoverText(stack: ItemStack, level: Level?, tooltipComponents: MutableList<Component>, isAdvanced: TooltipFlag) {
            tooltipComponents.add(
                Component.translatable("${stack.descriptionId}.description")
                .withStyle(
                    Style.EMPTY
                    .applyFormat(ChatFormatting.GOLD)
                ))
            super.appendHoverText(stack, level, tooltipComponents, isAdvanced)
        }
    }