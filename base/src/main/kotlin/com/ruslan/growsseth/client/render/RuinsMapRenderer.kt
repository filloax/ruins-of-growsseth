package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.ruslan.growsseth.config.ClientConfig
import com.ruslan.growsseth.maps.getMapTargetIcon
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.data.AtlasIds
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import java.util.function.Function


object RuinsMapRenderer {
    private val mapDecorations by lazy { Minecraft.getInstance().atlasManager }

    /**
     * Renders map icons in the corner of the ruins map.
     *
     * Assumes this is a RUINS_MAP, do the check before.
    */
    @JvmStatic
    fun ItemStack.renderRuinsMapIcon(pose: PoseStack, bufferSource: MultiBufferSource.BufferSource, x: Int, y: Int) {
        if (!ClientConfig.mapCornerIcons) return

        val mapIcon = this.getMapTargetIcon()?.type ?: return
        val texture = mapDecorations.getAtlasOrThrow(AtlasIds.MAP_DECORATIONS)
            .getSprite(mapIcon.value().assetId)

        renderCornerTexture(texture, bufferSource, pose, x, y)
    }

    // top left corner of slot
    private const val OFFSET_X = 4
    private const val OFFSET_Y = 4

    private var pose: PoseStack? = null
    private var bufferSource: MultiBufferSource.BufferSource? = null

    private fun renderCornerTexture(texture: TextureAtlasSprite, bufferSource: MultiBufferSource.BufferSource, pose: PoseStack, x: Int, y: Int) {
        this.pose = pose
        this.bufferSource = bufferSource

        pose.pushPose()

        // Items are z 150 at time of writing
        pose.translate(x.toDouble() + OFFSET_X, y.toDouble() + OFFSET_Y, 200.0)
//        pose.mulPose(Axis.ZP.rotationDegrees(180f))
        pose.scale(1.0f, 1.0f, 3.0f)

        // todo: fix
//        blitSprite(Function { location: Identifier -> RenderType.guiTextured(location) },
//            texture, 8, 8, 0, 0, -4, -4, 0, 8, 8)

        pose.popPose()
    }

    // Adapt base methods because I cannot figure rendering out [net.minecraft.client.gui.GuiGraphics]
    // TODO: there should be a way in newer versions to do this by data, adapting base methods requires too much work

    private fun blitSprite(
        renderTypeGetter: Function<Identifier?, RenderType>,
        sprite: TextureAtlasSprite,
        textureWidth: Int,
        textureHeight: Int,
        uPosition: Int,
        vPosition: Int,
        x: Int,
        y: Int,
        uWidth: Int,
        vHeight: Int,
        blitOffset: Int
    ) {
        if (uWidth != 0 && vHeight != 0) {
            this.innerBlit(
                renderTypeGetter,
                sprite.atlasIdentifier(),
                x,
                x + uWidth,
                y,
                y + vHeight,
                sprite.getU(uPosition.toFloat() / textureWidth),
                sprite.getU((uPosition + uWidth).toFloat() / textureWidth),
                sprite.getV(vPosition.toFloat() / textureHeight),
                sprite.getV((vPosition + vHeight).toFloat() / textureHeight),
                blitOffset
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
     * @param minU the minimum horizontal texture coordinate.
     * @param maxU the maximum horizontal texture coordinate.
     * @param minV the minimum vertical texture coordinate.
     * @param maxV the maximum vertical texture coordinate.
     */
    private fun innerBlit(
        renderTypeGetter: Function<Identifier?, RenderType>,
        atlasLocation: Identifier?,
        x1: Int,
        x2: Int,
        y1: Int,
        y2: Int,
        minU: Float,
        maxU: Float,
        minV: Float,
        maxV: Float,
        color: Int
    ) {
        val rendertype = renderTypeGetter.apply(atlasLocation)
        val matrix4f: Matrix4f = this.pose!!.last().pose()
        val vertexconsumer: VertexConsumer = this.bufferSource!!.getBuffer(rendertype)
        vertexconsumer.addVertex(matrix4f, x1.toFloat(), y1.toFloat(), 0.0f).setUv(minU, minV).setColor(color)
        vertexconsumer.addVertex(matrix4f, x1.toFloat(), y2.toFloat(), 0.0f).setUv(minU, maxV).setColor(color)
        vertexconsumer.addVertex(matrix4f, x2.toFloat(), y2.toFloat(), 0.0f).setUv(maxU, maxV).setColor(color)
        vertexconsumer.addVertex(matrix4f, x2.toFloat(), y1.toFloat(), 0.0f).setUv(maxU, minV).setColor(color)
    }
}