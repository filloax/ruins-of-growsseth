package com.ruslan.growsseth.item

import com.filloax.fxlib.api.FxUtils
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.RecordItem
import net.minecraft.world.item.TooltipFlag

class RecordCreditsItem(
    analogOutput: Int, sound: SoundEvent, properties: Properties, lengthInSeconds: Int,
    val authors: List<String>,
    val extra: List<Component> = listOf(),
) : RecordItem(analogOutput, sound, properties, lengthInSeconds) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag) // <-- RecordItem appends name

        if (authors.isNotEmpty() || extra.isNotEmpty())
            if (FxUtils.hasShiftDown()) {
                tooltipComponents.addAll(authors
                    .flatMap { splitStringToMaxLength(it, 30).mapIndexed { index, s -> if (index >= 1) "  $s" else s } }
                    .map { Component.literal(it).withStyle(ChatFormatting.BLUE) }
                )
                tooltipComponents.addAll(extra)
            } else {
                tooltipComponents.add(Component.translatable("item.growsseth.authors.pressShift").withStyle(ChatFormatting.BLUE))
            }
    }

    private fun splitStringToMaxLength(input: String, maxLength: Int): List<String> {
        val words = input.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if (currentLine.length + word.length > maxLength) {
                lines.add(currentLine.trim())
                currentLine = word
            } else {
                if (currentLine.isNotEmpty()) {
                    currentLine += " "
                }
                currentLine += word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.trim())
        }

        return lines
    }
}