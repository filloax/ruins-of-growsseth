package com.ruslan.growsseth.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.ruslan.growsseth.GrowssethAdvancements;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Locale;

@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {
    @Unique
    private boolean isNonTiledBackground(AdvancementTab tab) {
        String tabName = tab.getRootNode().holder().id().getPath().split("/")[0].strip().toLowerCase(Locale.ROOT);
        return GrowssethAdvancements.TABS_WITH_SINGLE_BACKGROUND.contains(tabName);
    }

    @WrapWithCondition(
        method = "drawContents",
        at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V")
    )
    private boolean drawContentsCancelBackgroundBlit(GuiGraphics instance, ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        AdvancementTab th1s = (AdvancementTab) (Object) this;
        return !isNonTiledBackground(th1s);
    }

    @Inject(
        method = "drawContents",
        at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;drawConnectivity(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void drawContentsCustomBackground(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci, ResourceLocation resourceLocation, int i, int j) {
        AdvancementTab th1s = (AdvancementTab) (Object) this;
        if (isNonTiledBackground(th1s)) {
            int k = 0; //i % 16; ignore scroll
            int l = 0; //j % 16; ignore scroll
            guiGraphics.blit(resourceLocation, k, l, 0, 0, 240, 120, 240, 120);
        }
    }
}
