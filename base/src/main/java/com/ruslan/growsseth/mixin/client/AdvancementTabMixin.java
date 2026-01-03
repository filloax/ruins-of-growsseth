package com.ruslan.growsseth.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.ruslan.growsseth.advancements.GrowssethAdvancements;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
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
        at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V")
    )
    private boolean drawContentsCancelBackgroundBlit(GuiGraphics instance, RenderPipeline pipeline, Identifier atlas, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        AdvancementTab th1s = (AdvancementTab) (Object) this;
        return !isNonTiledBackground(th1s);
    }

    @Inject(
        method = "drawContents",
        at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;drawConnectivity(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void drawContentsCustomBackground(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci, Identifier identifier, int i, int j) {
        AdvancementTab th1s = (AdvancementTab) (Object) this;
        if (isNonTiledBackground(th1s)) {
            int k = 0; //i % 16; ignore scroll
            int l = 0; //j % 16; ignore scroll
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, identifier, k, l, 0, 0, 240, 120, 240, 120);
        }
    }
}
