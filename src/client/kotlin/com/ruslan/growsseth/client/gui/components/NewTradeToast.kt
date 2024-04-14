package com.ruslan.growsseth.client.gui.components

import com.ruslan.growsseth.entity.researcher.trades.ResearcherItemListing
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastComponent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

// Adapted from RecipeToast
class NewTradeToast(newTrades: List<ResearcherItemListing>) : Toast {
    private val trades = newTrades.toMutableList()
    private var lastChanged: Long = 0
    private var changed = false

    companion object {
        private const val DISPLAY_TIME = 5000.0
        private val TITLE_TEXT: Component = Component.translatable("growsseth.notif.researcher_updated.toast.title")
        private val DESCRIPTION_TEXT: Component = Component.translatable("growsseth.notif.researcher_updated.toast.description")

        fun ToastComponent.updateNewTradeToast(trades: List<ResearcherItemListing>) {
            val tradeToast = getToast(NewTradeToast::class.java, Toast.NO_TOKEN)
            if (tradeToast == null) {
                addToast(NewTradeToast(trades))
            } else {
                tradeToast.addItems(trades)
            }
        }
    }

    override fun render(guiGraphics: GuiGraphics, toastComponent: ToastComponent, timeSinceLastVisible: Long): Toast.Visibility {
        if (changed) {
            lastChanged = timeSinceLastVisible
            changed = false
        }
        return if (trades.isEmpty()) {
            Toast.Visibility.HIDE
        } else {
            guiGraphics.blit(ResourceLocation("toast/recipe"), 0, 0, 0, 32, width(), height())
            guiGraphics.drawString(toastComponent.minecraft.font, TITLE_TEXT, 30, 7, -11534256, false)
            guiGraphics.drawString(toastComponent.minecraft.font, DESCRIPTION_TEXT, 30, 18, -16777216, false)
            val idx = (timeSinceLastVisible
                    / (DISPLAY_TIME * toastComponent.notificationDisplayTimeMultiplier / trades.size).coerceAtLeast(1.0)
                    % trades.size.toDouble()
                    ).toInt()
            val trade = trades[idx]
            guiGraphics.pose().pushPose()
            guiGraphics.renderFakeItem(trade.gives, 8, 8)
            guiGraphics.pose().popPose()
            if (timeSinceLastVisible - lastChanged >= DISPLAY_TIME * toastComponent.notificationDisplayTimeMultiplier)
                Toast.Visibility.HIDE
            else
                Toast.Visibility.SHOW
        }
    }

    private fun addItems(trades: List<ResearcherItemListing>) {
        this.trades.addAll(trades)
        changed = true
    }
}

