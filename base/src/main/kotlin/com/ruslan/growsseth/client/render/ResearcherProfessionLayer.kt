package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.ruslan.growsseth.utils.notNull
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ARGB

// Cloned from VillagerProfessionLayer
open class ResearcherProfessionLayer<T : LivingEntityRenderState, M : EntityModel<T>>(
    renderLayerParent: RenderLayerParent<T, M>,
    val typeTextureLocation: ResourceLocation,
    val profClothesLocation: ResourceLocation,
    val profClothesAggressiveLocation: ResourceLocation? = null,
    ) :
    RenderLayer<T, M>(renderLayerParent) {

    override fun render(
        poseStack: PoseStack,
        multiBufferSource: MultiBufferSource,
        bufferSource: Int,
        renderState: T,
        yRot: Float,
        xRot: Float
    ) {
        if (renderState.isInvisible) {
            return
        }

        renderColoredCutoutModel(
            parentModel, typeTextureLocation, poseStack, multiBufferSource,
            bufferSource, renderState,
            ARGB.colorFromFloat(1f, 1f, 1f, 1f)
        )

        if (renderState is ResearcherRendererState && renderState.isAggressive && notNull(profClothesAggressiveLocation)) {
            renderColoredCutoutModel(
                parentModel, profClothesAggressiveLocation, poseStack, multiBufferSource,
                bufferSource, renderState,
                ARGB.colorFromFloat(1f, 1f, 1f, 1f)
            )
        }
        else {
            renderColoredCutoutModel(
                parentModel, profClothesLocation, poseStack, multiBufferSource,
                bufferSource,renderState,
                ARGB.colorFromFloat(1f, 1f, 1f, 1f)
            )
        }
    }
}