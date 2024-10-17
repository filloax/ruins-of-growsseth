package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.ruslan.growsseth.config.ClientConfig
import com.ruslan.growsseth.maps.getMapTargetIcon
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f


object RuinsMapRenderer {
    private val mapDecorations by lazy { Minecraft.getInstance().mapDecorationTextures }

    /**
     * Renders map icons in the corner of the ruins map.
     *
     * Assumes this is a RUINS_MAP, do the check before.
    */
    @JvmStatic
    fun ItemStack.renderRuinsMapIcon(pose: PoseStack, bufferSource: MultiBufferSource.BufferSource, x: Int, y: Int) {
        if (!ClientConfig.mapCornerIcons) return

        val mapIcon = this.getMapTargetIcon()?.type ?: return
        val texture = mapDecorations.getSprite(mapIcon.value().assetId)

        renderCornerTexture(texture, bufferSource, pose, x, y)
    }

    // top left corner of slot
    private const val OFFSET_X = 4
    private const val OFFSET_Y = 4

    private fun renderCornerTexture(texture: TextureAtlasSprite, bufferSource: MultiBufferSource.BufferSource, pose: PoseStack, x: Int, y: Int) {
        pose.pushPose()

        // Items are z 150 at time of writing
        pose.translate(x.toDouble() + OFFSET_X, y.toDouble() + OFFSET_Y, 200.0)
//        pose.mulPose(Axis.ZP.rotationDegrees(180f))
        pose.scale(1.0f, 1.0f, 3.0f)

        blitSprite(pose, texture, 8, 8, 0, 0, -4, -4, 0, 8, 8)

        pose.popPose()
    }

    // Adapt base methods because I cannot figure rendering out

    private fun blitSprite(
        pose: PoseStack,
        sprite: TextureAtlasSprite,
        textureWidth: Int,
        textureHeight: Int,
        uPosition: Int,
        vPosition: Int,
        x: Int,
        y: Int,
        blitOffset: Int,
        uWidth: Int,
        vHeight: Int
    ) {
        if (uWidth != 0 && vHeight != 0) {
            this.innerBlit(
                pose, sprite.atlasLocation(),
                x, x + uWidth,
                y, y + vHeight,
                blitOffset,
                sprite.getU(uPosition.toFloat() / textureWidth),
                sprite.getU((uPosition + uWidth).toFloat() / textureWidth),
                sprite.getV(vPosition.toFloat() / textureHeight),
                sprite.getV((vPosition + vHeight).toFloat() / textureHeight)
            )
        }
    }

    /**
     * From [net.minecraft.client.gui.GuiGraphics]
     *
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates without color tinting.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1 the x-coordinate of the first corner of the blit position.
     * @param x2 the x-coordinate of the second corner of the blit position.
     * @param y1 the y-coordinate of the first corner of the blit position.
     * @param y2 the y-coordinate of the second corner of the blit position.
     * @param blitOffset the z-level offset for rendering order.
     * @param minU the minimum horizontal texture coordinate.
     * @param maxU the maximum horizontal texture coordinate.
     * @param minV the minimum vertical texture coordinate.
     * @param maxV the maximum vertical texture coordinate.
     */
    fun innerBlit(
        pose: PoseStack,
        atlasLocation: ResourceLocation,
        x1: Int,
        x2: Int,
        y1: Int,
        y2: Int,
        blitOffset: Int,
        minU: Float,
        maxU: Float,
        minV: Float,
        maxV: Float
    ) {
        RenderSystem.setShaderTexture(0, atlasLocation)
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        val matrix4f: Matrix4f = pose.last().pose()
        val bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        bufferBuilder.addVertex(matrix4f, x1.toFloat(), y1.toFloat(), blitOffset.toFloat()).setUv(minU, minV)
        bufferBuilder.addVertex(matrix4f, x1.toFloat(), y2.toFloat(), blitOffset.toFloat()).setUv(minU, maxV)
        bufferBuilder.addVertex(matrix4f, x2.toFloat(), y2.toFloat(), blitOffset.toFloat()).setUv(maxU, maxV)
        bufferBuilder.addVertex(matrix4f, x2.toFloat(), y1.toFloat(), blitOffset.toFloat()).setUv(maxU, minV)
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow())
    }
}