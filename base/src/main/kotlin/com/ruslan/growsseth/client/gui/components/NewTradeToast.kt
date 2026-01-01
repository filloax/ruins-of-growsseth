package com.ruslan.growsseth.client.gui.components

import com.ruslan.growsseth.entity.researcher.trades.ResearcherItemListing
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import kotlin.math.max

// Adapted from RecipeToast
class NewTradeToast(newTrades: List<ResearcherItemListing>) : Toast {
    private val trades = newTrades.toMutableList()
    private val tradeItems = newTrades.map { it.gives() }.toMutableList()
    private var lastChanged: Long = 0
    private var changed = false
    private var wantedVisibility = Toast.Visibility.HIDE
    private var displayedRecipeIndex = 0

    companion object {
        private const val DISPLAY_TIME = 5000.0
        private val TITLE_TEXT = Component.translatable("growsseth.notif.researcher_updated.toast.title")
        private val DESCRIPTION_TEXT = Component.translatable("growsseth.notif.researcher_updated.toast.description")
        private val BACKGROUND_SPRITE = ResourceLocation.parse("toast/recipe")

        fun ToastManager.updateNewTradeToast(trades: List<ResearcherItemListing>) {
            val tradeToast = getToast(NewTradeToast::class.java, Toast.NO_TOKEN)
            if (tradeToast == null) {
                addToast(NewTradeToast(trades))
            } else {
                tradeToast.addItems(trades)
            }
        }
    }

    private fun addItems(trades: List<ResearcherItemListing>) {
        this.trades.addAll(trades)
        this.tradeItems.addAll(trades.map{it.gives()})
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

        if (this.tradeItems.isEmpty()) {
            this.wantedVisibility = Toast.Visibility.HIDE
        } else {
            this.wantedVisibility =
                if ((visibilityTime - this.lastChanged).toDouble() >= 5000.0 * toastManager.notificationDisplayTimeMultiplier)
                    Toast.Visibility.HIDE
                else
                    Toast.Visibility.SHOW
        }

        this.displayedRecipeIndex = (visibilityTime.toDouble() / max(
            1.0,
            5000.0 * toastManager.notificationDisplayTimeMultiplier / this.tradeItems.size.toDouble()
        )
                % this.tradeItems.size.toDouble()
                ).toInt()
    }

    override fun render(guiGraphics: GuiGraphics, font: Font, visibilityTime: Long) {
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND_SPRITE,
            0,
            0,
            this.width(),
            this.height()
        )
        guiGraphics.drawString(font, TITLE_TEXT, 30, 7, -11534256, false)
        guiGraphics.drawString(font, DESCRIPTION_TEXT, 30, 18, -16777216, false)
        val itemStack = this.tradeItems[this.displayedRecipeIndex]
        guiGraphics.pose().pushMatrix()
        guiGraphics.pose().scale(0.6f, 0.6f)
        guiGraphics.renderFakeItem(ItemStack.EMPTY, 3, 3)
        guiGraphics.pose().popMatrix()
        guiGraphics.renderFakeItem(itemStack, 8, 8)
    }
}
