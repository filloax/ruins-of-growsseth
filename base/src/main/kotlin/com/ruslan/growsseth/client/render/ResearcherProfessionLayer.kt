package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.ruslan.growsseth.utils.notNull
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

// Cloned from VillagerProfessionLayer
open class ResearcherProfessionLayer<T : LivingEntityRenderState, M : EntityModel<T>>(
    renderLayerParent: RenderLayerParent<T, M>,
    val typeTextureLocation: Identifier,
    val profClothesLocation: Identifier,
    val profClothesAggressiveLocation: Identifier? = null,
    ) : RenderLayer<T, M>(renderLayerParent) {

    override fun submit(
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        packedLight: Int,
        renderState: T,
        yRot: Float,
        xRot: Float
    ) {
        if (renderState.isInvisible) {
            return
        }

        renderColoredCutoutModel(
            parentModel, typeTextureLocation, poseStack, nodeCollector,
            packedLight, renderState,
            ARGB.colorFromFloat(1f, 1f, 1f, 1f),
            1
        )

        if (renderState is ResearcherRendererState && renderState.isAggressive && notNull(profClothesAggressiveLocation)) {
            renderColoredCutoutModel(
                parentModel, profClothesAggressiveLocation, poseStack, nodeCollector,
                packedLight, renderState,
                ARGB.colorFromFloat(1f, 1f, 1f, 1f),
                1
            )
        }
        else {
            renderColoredCutoutModel(
                parentModel, profClothesLocation, poseStack, nodeCollector,
                packedLight, renderState,
                ARGB.colorFromFloat(1f, 1f, 1f, 1f),
                1
            )
        }
    }
}