package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.ruslan.growsseth.client.model.ResearcherModel
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.client.model.ArmedModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.Items

class ResearcherRenderer(context: EntityRendererProvider.Context)
    : MobRenderer<Researcher, ResearcherRendererState, ResearcherModel>(
    context,
    ResearcherModel(context.bakeLayer(ModelLayers.VINDICATOR)),
    0.5f    // Same as other villagers and illagers
) {
    companion object {
        private val RESEARCHER_SKIN = resLoc("textures/entity/illager/researcher.png")
        private val RESEARCHER_SKIN_ANGRY = resLoc("textures/entity/illager/researcher_angry.png")
        private val RESEARCHER_TYPE_SKIN = resLoc("textures/entity/villager/type/researcher.png")
        private val RESEARCHER_CLOTHES = resLoc("textures/entity/villager/profession/researcher.png")
        private val RESEARCHER_CLOTHES_UNSHEATED_DAGGER = resLoc("textures/entity/villager/profession/researcher_unsheated.png")
    }

    override fun createRenderState(): ResearcherRendererState {
        return ResearcherRendererState()
    }

    override fun extractRenderState(researcher: Researcher, reusedState: ResearcherRendererState, partialTick: Float) {
        super.extractRenderState(researcher, reusedState, partialTick)
        reusedState.mainHandItem = researcher.mainHandItem
        reusedState.isUsingItem = researcher.isUsingItem
        reusedState.unhappyCounter = researcher.unhappyCounter
        reusedState.isAggressive = researcher.isAggressive
        reusedState.armPose = researcher.armPose
        reusedState.attackAnim = researcher.getAttackAnim(partialTick)
    }

    override fun getTextureLocation(renderState: ResearcherRendererState): ResourceLocation {
        return if (renderState.isAggressive)
            RESEARCHER_SKIN_ANGRY
        else
            RESEARCHER_SKIN
    }

    init {
        addLayer(CustomHeadLayer(this, context.modelSet))

        addLayer(
            ResearcherProfessionLayer<ResearcherRendererState, ResearcherModel>
            (
                this, RESEARCHER_TYPE_SKIN,
                RESEARCHER_CLOTHES, RESEARCHER_CLOTHES_UNSHEATED_DAGGER
            )
        )

        addLayer(object : ItemInHandLayer<ResearcherRendererState, ResearcherModel>(this) {
            override fun renderArmWithItem(
                researcherRenderState: ResearcherRendererState,
                itemStackRenderState: ItemStackRenderState,
                arm: HumanoidArm, poseStack: PoseStack,
                buffer: MultiBufferSource, packedLight: Int
            ) {
                val heldItem = researcherRenderState.mainHandItem
                if (
                    researcherRenderState.isAggressive &&
                    heldItem.`is`(GrowssethItems.RESEARCHER_DAGGER)
                ) {
                    // The dagger is held upside down, differently from other items (original code adapted from Vindicator)
                    poseStack.pushPose()
                    (this.parentModel as ArmedModel).translateToHand(arm, poseStack)
                    poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))     // 90° instead of -90°
                    //poseStack.mulPose(Axis.YP.rotationDegrees(180.0f))    // no y rotation
                    poseStack.translate(-0.1, 0.0, 0.0)        // centering the dagger inside the hand
                    val isLeftArm = (arm == HumanoidArm.LEFT)
                    poseStack.translate((if (isLeftArm) -1 else 1).toFloat() / 16.0f, 0.125f, -0.625f)
                    itemStackRenderState.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY)
                    poseStack.popPose()
                }
                else if (
                    researcherRenderState.isAggressive ||
                    heldItem.`is`(Items.POTION) ||
                    heldItem[DataComponents.FOOD] != null ||
                    heldItem.`is`(Items.ENDER_PEARL)
                ) {
                    // Other cases when he keeps his arm up are rendered normally
                    super.renderArmWithItem(researcherRenderState, itemStackRenderState, arm, poseStack, buffer, packedLight)
                }
            }
        })
    }
}
