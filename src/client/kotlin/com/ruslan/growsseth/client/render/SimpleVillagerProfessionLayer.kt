package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.world.entity.LivingEntity

// cloned from VillagerProfessionLayer.
// Unused, meant for researcher originally
class SimpleVillagerProfessionLayer<T : LivingEntity, M : EntityModel<T>>(
    renderLayerParent: RenderLayerParent<T, M>,
    val typeTextureLocation: ResourceLocation,
    val profClothesLocation: ResourceLocation,
) :
    RenderLayer<T, M>(renderLayerParent) {

    override fun render(
        poseStack: PoseStack, multiBufferSource: MultiBufferSource, i: Int,
        entity: T,
        f: Float, g: Float, h: Float,
        j: Float, k: Float, l: Float,
    ) {
        if (entity.isInvisible) {
            return
        }

        renderColoredCutoutModel(
            parentModel, typeTextureLocation, poseStack, multiBufferSource,
            i, entity,
            FastColor.ARGB32.colorFromFloat(1f, 1f, 1f, 1f)
        )

        renderColoredCutoutModel(
            parentModel, profClothesLocation, poseStack, multiBufferSource,
            i,entity,
            FastColor.ARGB32.colorFromFloat(1f, 1f, 1f, 1f)
        )
    }
}