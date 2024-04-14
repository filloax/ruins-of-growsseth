package com.ruslan.growsseth.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ruslan.growsseth.interfaces.WithCustomDecorations;
import com.ruslan.growsseth.maps.CustomMapDecoration;
import com.ruslan.growsseth.maps.CustomMapDecorationType;
import com.ruslan.growsseth.maps.MapIconTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Objects;

/**
 * Add rendering code to maps to also render our custom icons on top
 */
@Mixin(targets = "net.minecraft.client.gui.MapRenderer$MapInstance")
public abstract class MapRendererInstanceMixin {
    @Shadow
    private MapItemSavedData data;

    @Inject(at = @At("TAIL"), method = "draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ZI)V")
    private void drawCustomMapDecorations(PoseStack poseStack, MultiBufferSource multiBufferSource, boolean hideOutOfFrame, int i, CallbackInfo ci) {
        int zOffset = 32;

        Iterator<CustomMapDecoration> customDecorations = ((WithCustomDecorations)this.data).getCustomDecorations().iterator();

        while(true) {
            CustomMapDecoration customDecoration;
            do {
                if (!customDecorations.hasNext()) {
                    return;
                }

                customDecoration = customDecorations.next();
            } while(hideOutOfFrame && !customDecoration.renderOnFrame());

            poseStack.pushPose();
            poseStack.translate((float)customDecoration.getX() / 2.0F + 64.0F, (float)customDecoration.getY() / 2.0F + 64.0F, -0.02F);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)(customDecoration.getRot() * 360) / 16.0F));
            poseStack.scale(4.0F, 4.0F, 3.0F);
            poseStack.translate(-0.125F, 0.125F, 0.0F);
            CustomMapDecorationType type = customDecoration.getType();
            byte iconId = (byte) type.getIconNum();
            int iconsPerRow = type.getIconsPerRow();
            float leftX = (float)(iconId % iconsPerRow) / (float) iconsPerRow;
            float topY = (float)(iconId / iconsPerRow) / (float) iconsPerRow;
            float rightX = (float)(iconId % iconsPerRow + 1) / (float) iconsPerRow;
            float bottomY = (float)(iconId / iconsPerRow + 1) / (float) iconsPerRow;
            Matrix4f matrix4f2 = poseStack.last().pose();
            VertexConsumer vertexConsumer2 = multiBufferSource.getBuffer(MapIconTextures.INSTANCE.getCustomMapDecorationTextures().get(type.getId()));
            vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, (float)zOffset * -0.001F).color(255, 255, 255, 255).uv(leftX, topY).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, (float)zOffset * -0.001F).color(255, 255, 255, 255).uv(rightX, topY).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, (float)zOffset * -0.001F).color(255, 255, 255, 255).uv(rightX, bottomY).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, (float)zOffset * -0.001F).color(255, 255, 255, 255).uv(leftX, bottomY).uv2(i).endVertex();
            poseStack.popPose();
            if (customDecoration.getName() != null) {
                Font font = Minecraft.getInstance().font;
                Component component = customDecoration.getName();
                float p = (float)font.width(component);
                float var10000 = 25.0F / p;
                Objects.requireNonNull(font);
                float q = Mth.clamp(var10000, 0.0F, 6.0F / 9.0F);
                poseStack.pushPose();
                poseStack.translate(0.0F + (float)customDecoration.getX() / 2.0F + 64.0F - p * q / 2.0F, 0.0F + (float)customDecoration.getY() / 2.0F + 64.0F + 4.0F, -0.025F);
                poseStack.scale(q, q, 1.0F);
                poseStack.translate(0.0F, 0.0F, -0.1F);
                font.drawInBatch(component, 0.0F, 0.0F, -1, false, poseStack.last().pose(), multiBufferSource, Font.DisplayMode.NORMAL, Integer.MIN_VALUE, i);
                poseStack.popPose();
            }

            ++zOffset;
        }
    }
}
