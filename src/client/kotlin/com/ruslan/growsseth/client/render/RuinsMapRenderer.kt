package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.ruslan.growsseth.config.ClientConfig
import com.ruslan.growsseth.maps.getMapTargetIcon
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.item.ItemStack

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
        pose.mulPose(Axis.ZP.rotationDegrees(180f))
        pose.scale(4.0f, 4.0f, 3.0f)
        pose.translate(-0.125f, 0.125f, 0.0f)
        val poseMatrix = pose.last().pose()
        val h = texture.u0 + 0.0001f
        val l = texture.v0 + 0.0001f
        val m = texture.u1 - 0.0001f
        val n = texture.v1 - 0.0001f
        val zOffset = 0f
        val buffer = bufferSource.getBuffer(RenderType.text(texture.atlasLocation()))
        val light = 15728880
        buffer.vertex(poseMatrix, -1.0f, 1.0f, zOffset)
            .color(255, 255, 255, 255)
            .uv(h, l)
            .uv2(light)
            .endVertex()
        buffer.vertex(poseMatrix, 1.0f, 1.0f, zOffset)
            .color(255, 255, 255, 255)
            .uv(m, l)
            .uv2(light)
            .endVertex()
        buffer.vertex(poseMatrix, 1.0f, -1.0f, zOffset)
            .color(255, 255, 255, 255)
            .uv(m, n)
            .uv2(light)
            .endVertex()
        buffer.vertex(poseMatrix, -1.0f, -1.0f, zOffset)
            .color(255, 255, 255, 255)
            .uv(h, n)
            .uv2(light)
            .endVertex()
        pose.popPose()
    }
}