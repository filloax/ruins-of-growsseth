package com.ruslan.growsseth.client.gui.components

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastComponent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence

class CustomTextToast private constructor(
    private var title: Component,
    private var messageLines: List<FormattedCharSequence>,
    private val width: Int,
    val bgTexture: ResourceLocation = ResourceLocation("toast/system"),
) : Toast {
    private var lastChanged: Long = 0
    private var changed = false

    companion object {
        private const val DISPLAY_TIME = 5000L
        private const val MAX_LINE_SIZE = 200
        private const val LINE_SPACING = 12
        private const val MARGIN = 10
        private const val MAX_SLOTS = 5

        fun multiline(font: Font, title: Component, message: Component? = null): CustomTextToast {
            val list = checkNullAndSplit(message, font)
            val width = list.stream().mapToInt { font.width(it) }
                .max().orElse(MAX_LINE_SIZE).coerceAtLeast(MAX_LINE_SIZE)
            return CustomTextToast(title, list, width + 30)
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

    constructor(component: Component, component2: Component?) : this(
        component,
        checkNullAndSplit(component2),
        (30 + Minecraft.getInstance().font.width(component)
            .coerceAtLeast(if (component2 == null) 0 else Minecraft.getInstance().font.width(component2))).coerceAtLeast(160)
    )

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
            guiGraphics.blit(bgTexture, 0, 0, 0, 64, i, height())
        } else {
            val j = height()
            val k = 28
            val l = Math.min(4, j - 28)
            renderBackgroundRow(guiGraphics, toastComponent, i, 0, 0, 28)
            var m = 28
            while (m < j - l) {
                renderBackgroundRow(guiGraphics, toastComponent, i, 16, m, Math.min(16, j - m - l))
                m += MARGIN
            }
            renderBackgroundRow(guiGraphics, toastComponent, i, 32 - l, j - l, l)
        }

        if (messageLines.isEmpty()) {
            guiGraphics.drawString(toastComponent.minecraft.font, title, 18, LINE_SPACING, -256, false)
        } else {
            guiGraphics.drawString(toastComponent.minecraft.font, title, 18, 7, -256, false)
            for (j in messageLines.indices) {
                guiGraphics.drawString(toastComponent.minecraft.font, messageLines[j], 18, 18 + j * LINE_SPACING, -1, false)
            }
        }
        return if ((timeSinceLastVisible - lastChanged) < DISPLAY_TIME * toastComponent.notificationDisplayTimeMultiplier)
            Toast.Visibility.SHOW
        else
            Toast.Visibility.HIDE
    }

    private fun renderBackgroundRow(
        guiGraphics: GuiGraphics,
        toastComponent: ToastComponent,
        i: Int,
        j: Int,
        k: Int,
        l: Int,
    ) {
        val m = if (j == 0) 20 else 5
        val n = Math.min(60, i - m)
        guiGraphics.blit(bgTexture, 0, k, 0, 64 + j, m, l)
        var o = m
        while (o < i - n) {
            guiGraphics.blit(bgTexture, o, k, 32, 64 + j, Math.min(64, i - o - n), l)
            o += 64
        }
        guiGraphics.blit(bgTexture, i - n, k, 160 - n, 64 + j, n, l)
    }

    fun reset(title: Component, message: Component?, fontToSplit: Font? = null) {
        this.title = title
        messageLines = checkNullAndSplit(message, fontToSplit)
        changed = true
    }
}
