package com.ruslan.growsseth.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ruslan.growsseth.client.render.RuinsMapRenderer;
import com.ruslan.growsseth.item.GrowssethItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class RuinsMapIcon_GuiGraphicsMixin {
    @Final
    @Shadow
    private PoseStack pose;

    @Shadow
    public abstract MultiBufferSource.BufferSource bufferSource();

    @Inject(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            shift = At.Shift.AFTER
        )
    )
    private void onRenderItemDecorations(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (stack.is(GrowssethItems.RUINS_MAP)) {
            RuinsMapRenderer.renderRuinsMapIcon(stack, pose, bufferSource(), x, y);
        }
    }
}
