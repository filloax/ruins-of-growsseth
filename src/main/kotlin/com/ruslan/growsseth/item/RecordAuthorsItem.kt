package com.ruslan.growsseth.item

import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.RecordItem
import net.minecraft.world.item.TooltipFlag

class RecordAuthorsItem(
    analogOutput: Int, sound: SoundEvent, properties: Properties, lengthInSeconds: Int,
    val authors: List<String>,
) : RecordItem(analogOutput, sound, properties, lengthInSeconds) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag) // <-- RecordItem appends name

        // TODO: add authors with shift
    }
}