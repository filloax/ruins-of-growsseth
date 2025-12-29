package com.ruslan.growsseth.client.gui.components

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import java.util.function.Function
import kotlin.math.min

// Edit of SystemToast
class CustomTextToast private constructor(
    private var title: Component,
    private var messageLines: List<FormattedCharSequence>,
    private val width: Int,
) : Toast {
    private var lastChanged: Long = 0
    private var changed = false
    private var wantedVisibility = Toast.Visibility.HIDE
    private var forceHide = false

    companion object {
        private const val DISPLAY_TIME = 5000L
        private const val MAX_LINE_SIZE = 200
        private const val LINE_SPACING = 12
        private const val MARGIN = 10
        private const val MAX_SLOTS = 5
        private val BACKGROUND_SPRITE: ResourceLocation = ResourceLocation.withDefaultNamespace("toast/system")

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


    private fun renderBackgroundRow(guiGraphics: GuiGraphics, width: Int, vOffset: Int, y: Int, height: Int) {
        val i = if (vOffset == 0) 20 else 5
        val j = min(60, width - i)
        val resourcelocation = BACKGROUND_SPRITE
        guiGraphics.blitSprite(
            { location: ResourceLocation -> RenderType.guiTextured(location) },
            resourcelocation,
            160,
            32,
            0,
            vOffset,
            0,
            y,
            i,
            height
        )

        var k = i
        while (k < width - j) {
            guiGraphics.blitSprite(
                { location: ResourceLocation -> RenderType.guiTextured(location) },
                resourcelocation,
                160,
                32,
                32,
                vOffset,
                k,
                y,
                min(64, width - k - j),
                height
            )
            k += 64
        }

        guiGraphics.blitSprite(
            { location: ResourceLocation -> RenderType.guiTextured(location) },
            resourcelocation,
            160,
            32,
            160 - j,
            vOffset,
            width - j,
            y,
            j,
            height
        )
    }

    fun reset(title: Component, message: Component?, fontToSplit: Font? = null) {
        this.title = title
        messageLines = checkNullAndSplit(message, fontToSplit)
        changed = true
    }


    override fun getWantedVisibility(): Toast.Visibility {
        return this.wantedVisibility
    }


    override fun update(toastManager: ToastManager, visibilityTime: Long) {
        if (this.changed) {
            this.lastChanged = visibilityTime
            this.changed = false
        }

        // We don't use the id property, just our display time
        val d0 = DISPLAY_TIME * toastManager.getNotificationDisplayTimeMultiplier()
        val i = visibilityTime - this.lastChanged
        this.wantedVisibility =
            if (!this.forceHide && i.toDouble() < d0) Toast.Visibility.SHOW else Toast.Visibility.HIDE
    }


    override fun render(guiGraphics: GuiGraphics, font: Font, visibilityTime: Long) {
        val i = this.width()
        if (i == 160 && this.messageLines.size <= 1) {
            guiGraphics.blitSprite(
                { location: ResourceLocation -> RenderType.guiTextured(location) },
                BACKGROUND_SPRITE,
                0,
                0,
                i,
                this.height()
            )
        } else {
            val j = this.height()
            val k = 28
            val l = min(4, j - 28)
            this.renderBackgroundRow(guiGraphics, i, 0, 0, 28)

            var i1 = 28
            while (i1 < j - l) {
                this.renderBackgroundRow(guiGraphics, i, 16, i1, min(16, j - i1 - l))
                i1 += 10
            }

            this.renderBackgroundRow(guiGraphics, i, 32 - l, j - l, l)
        }

        if (this.messageLines.isEmpty()) {
            guiGraphics.drawString(font, this.title, 18, 12, -256, false)
        } else {
            guiGraphics.drawString(font, this.title, 18, 7, -256, false)

            for (j1 in this.messageLines.indices) {
                guiGraphics.drawString(font, this.messageLines.get(j1), 18, 18 + j1 * 12, -1, false)
            }
        }
    }

}
