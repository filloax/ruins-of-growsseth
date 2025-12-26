package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.ruslan.growsseth.client.model.ResearcherModel
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.item.AbstractResearcherDaggerItem
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
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.PotionItem

class ResearcherRenderer(context: EntityRendererProvider.Context)
    : MobRenderer<Researcher, ResearcherRendererState, ResearcherModel>(
    context,
    ResearcherModel(context.bakeLayer(ModelLayers.VINDICATOR)),
    0.5f
) {
    init {
        addLayer(CustomHeadLayer(this, context.modelSet))
        addLayer(ResearcherProfessionLayer<ResearcherRendererState, ResearcherModel>(
            this, RESEARCHER_TYPE_SKIN,
            RESEARCHER_CLOTHES, RESEARCHER_CLOTHES_UNSHEATED_DAGGER
        ))
        addLayer(object : ItemInHandLayer<ResearcherRendererState, ResearcherModel>(this) {
            override fun renderArmWithItem(
                renderState: ResearcherRendererState,
                itemStackRenderState: ItemStackRenderState,
                arm: HumanoidArm,
                poseStack: PoseStack,
                buffer: MultiBufferSource,
                packedLight: Int
            ) {
                val researcher = renderState.entity
                researcher as Researcher
                val researcherItem = researcher.mainHandItem
                if ((researcherItem is AbstractResearcherDaggerItem) && researcher.isAggressive) {
                    poseStack.pushPose()
                    (this.parentModel as ArmedModel).translateToHand(arm, poseStack)
                    poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))     // 90 instead of -90
                    //poseStack.mulPose(Axis.YP.rotationDegrees(180.0f))    // no y rotation
                    poseStack.translate(-0.1, 0.0, 0.0)             // centering the dagger inside the hand
                    val bl = arm == HumanoidArm.LEFT
                    poseStack.translate((if (bl) -1 else 1).toFloat() / 16.0f, 0.125f, -0.625f)
                    itemStackRenderState.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY)
                    poseStack.popPose()
                }
                else if (researcher.isAggressive || researcherItem is PotionItem || researcherItem[DataComponents.FOOD] != null || researcherItem.`is`(Items.ENDER_PEARL))
                    super.renderArmWithItem(renderState, itemStackRenderState, arm, poseStack, buffer, packedLight)
            }
        })
    }

    override fun getTextureLocation(renderState: ResearcherRendererState): ResourceLocation {
        return if (renderState.entity!!.isAggressive)
            RESEARCHER_SKIN_ANGRY
        else
            RESEARCHER_SKIN
    }

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

    override fun extractRenderState(entity: Researcher, reusedState: ResearcherRendererState, partialTick: Float) {
        super.extractRenderState(entity, reusedState, partialTick)
        reusedState.entity = entity
        reusedState.attackAnim = entity.getAttackAnim(partialTick)
    }
}
