package com.ruslan.growsseth.client.gui.components

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.toasts.AdvancementToast
import net.minecraft.client.gui.components.toasts.RecipeToast
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastComponent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import kotlin.math.min

class CustomTextItemToast private constructor(
    private var title: Component,
    private var messageLines: List<FormattedCharSequence>,
    private var item: ItemStack,
    private val width: Int,
    val bgTexture: ResourceLocation = ResourceLocation.parse("toast/advancement"),
) : Toast {
    private var lastChanged: Long = 0
    private var changed = false

    companion object {
        private const val DISPLAY_TIME = 5000L
        private const val MAX_LINE_SIZE = 200 - 12 // shorter because item
        private const val LINE_SPACING = 12
        private const val MARGIN = 10
        private const val MAX_SLOTS = 5

        fun multiline(font: Font, title: Component, item: ItemStack, message: Component? = null): CustomTextItemToast {
            val list = checkNullAndSplit(message, font)
            val width = list.stream().mapToInt { font.width(it) }
                .max().orElse(MAX_LINE_SIZE).coerceAtLeast(MAX_LINE_SIZE)
            return CustomTextItemToast(title, list, item, width + 30)
        }

        private fun checkNullAndSplit(message: Component?, splitFont: Font? = null): List<FormattedCharSequence> {
            return message?.let {
                if (splitFont != null)
                    splitFont.split(it, MAX_LINE_SIZE)
                else
                    listOf(it.visualOrderText)
            } ?: listOf()
        }
    }

    override fun width(): Int {
        return width
    }

    override fun height(): Int {
        return (20 + messageLines.size.coerceAtLeast(1) * LINE_SPACING).coerceAtMost(32 * MAX_SLOTS)
    }

    override fun render(guiGraphics: GuiGraphics, toastComponent: ToastComponent, timeSinceLastVisible: Long): Toast.Visibility {
        if (changed) {
            lastChanged = timeSinceLastVisible
            changed = false
        }
        val i = width()
        if (i == 160 && messageLines.size <= 1) {
            guiGraphics.blitSprite(bgTexture, 0, 0, i, height())
        } else {
            val j = height()
            val k = 28
            val l = min(4, j - k)
            renderBackgroundRow(guiGraphics, i, 0, 0, k)

            for (m in k until j - l step MARGIN) {
                renderBackgroundRow(guiGraphics, i, 16, m, min(16, j - m - l))
            }
            renderBackgroundRow(guiGraphics, i, 32 - l, j - l, l)
        }

        if (messageLines.isEmpty()) {
            guiGraphics.drawString(toastComponent.minecraft.font, title, 30, LINE_SPACING, -256, false)
        } else {
            guiGraphics.drawString(toastComponent.minecraft.font, title, 30, 7, -256, false)
            for (j in messageLines.indices) {
                guiGraphics.drawString(toastComponent.minecraft.font, messageLines[j], 30, 18 + j * LINE_SPACING, -1, false)
            }
        }
        guiGraphics.pose().pushPose()
        guiGraphics.renderFakeItem(item, 8, 8)
        guiGraphics.pose().popPose()
        return if ((timeSinceLastVisible - lastChanged) < DISPLAY_TIME * toastComponent.notificationDisplayTimeMultiplier)
            Toast.Visibility.SHOW
        else
            Toast.Visibility.HIDE
    }

    private fun renderBackgroundRow(guiGraphics: GuiGraphics, width: Int, vOffset: Int, y: Int, height: Int) {
        val i = if (vOffset == 0) 20 else 5
        val j = min(60, (width - i))
        val texturePath = bgTexture
        guiGraphics.blitSprite(texturePath, 160, 32, 0, vOffset, 0, y, i, height)

        var k = i
        while (k < width - j) {
            guiGraphics.blitSprite(
                texturePath, 160, 32, 32, vOffset, k, y,
                min(64.0, (width - k - j).toDouble()).toInt(), height
            )
            k += 64
        }

        guiGraphics.blitSprite(texturePath, 160, 32, 160 - j, vOffset, width - j, y, j, height)
    }

    fun reset(title: Component, item: ItemStack, message: Component?, fontToSplit: Font? = null) {
        this.title = title
        this.item = item
        messageLines = checkNullAndSplit(message, fontToSplit)
        changed = true
    }
}
