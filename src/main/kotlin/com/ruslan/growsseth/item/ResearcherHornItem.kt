package com.ruslan.growsseth.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Instrument
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class ResearcherHornItem(properties: Properties, instruments: TagKey<Instrument>) : InstrumentItem(properties, instruments) {
    override fun appendHoverText(stack: ItemStack, level: Level?, tooltipComponents: MutableList<Component>, isAdvanced: TooltipFlag) {
        for (i in 1 .. 2) {
            tooltipComponents.add(Component.translatable("${stack.descriptionId}.description$i")
                .withStyle(Style.EMPTY
                    .applyFormat(ChatFormatting.BLUE)
                    .applyFormat(ChatFormatting.ITALIC)
                ))
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced)
    }
}