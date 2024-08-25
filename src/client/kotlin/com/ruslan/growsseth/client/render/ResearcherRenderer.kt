package com.ruslan.growsseth.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.ruslan.growsseth.client.model.ResearcherModel
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.item.ResearcherDaggerItem
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.client.model.ArmedModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.PotionItem

class ResearcherRenderer(context: EntityRendererProvider.Context) : MobRenderer<Researcher, ResearcherModel>(
    context, ResearcherModel(context.bakeLayer(ModelLayers.VINDICATOR)), 0.5f
) {
    init {
        addLayer(CustomHeadLayer(this, context.modelSet, context.itemInHandRenderer))
        addLayer(ResearcherProfessionLayer<Researcher, ResearcherModel>(
            this, RESEARCHER_TYPE_SKIN, RESEARCHER_CLOTHES, RESEARCHER_CLOTHES_UNSHEATED_DAGGER
        ))
        addLayer(object : ItemInHandLayer<Researcher, ResearcherModel>(this, context.itemInHandRenderer) {
            override fun renderArmWithItem (
                researcher: LivingEntity, itemStack: ItemStack, displayContext: ItemDisplayContext,
                arm: HumanoidArm, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int
            ) {
                researcher as Researcher
                if (itemStack.item is ResearcherDaggerItem && researcher.isAggressive) {
                    poseStack.pushPose()
                    (this.parentModel as ArmedModel).translateToHand(arm, poseStack)
                    poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))     // 90 instead of -90
                    //poseStack.mulPose(Axis.YP.rotationDegrees(180.0f))    // no y rotation
                    poseStack.translate(-0.1, 0.0, 0.0)             // centering the dagger inside the hand
                    val bl = arm == HumanoidArm.LEFT
                    poseStack.translate((if (bl) -1 else 1).toFloat() / 16.0f, 0.125f, -0.625f)
                    context.itemInHandRenderer.renderItem(researcher,  itemStack, displayContext, bl, poseStack, buffer, packedLight)
                    poseStack.popPose()
                }
                else if (researcher.isAggressive || itemStack.item is PotionItem || itemStack[DataComponents.FOOD] != null || itemStack.`is`(Items.ENDER_PEARL))
                    super.renderArmWithItem(researcher, itemStack, displayContext, arm, poseStack, buffer, packedLight)
            }
        })
    }

    override fun getTextureLocation(entity: Researcher): ResourceLocation {
        return if (entity.isAggressive)
            RESEARCHER_SKIN_ANGRY
        else
            RESEARCHER_SKIN
    }

    companion object {
        private val RESEARCHER_SKIN = resLoc("textures/entity/illager/researcher.png")
        private val RESEARCHER_SKIN_ANGRY = resLoc("textures/entity/illager/researcher_angry.png")
        private val RESEARCHER_TYPE_SKIN = ResourceLocation("minecraft", "textures/entity/villager/type/plains.png")
        private val RESEARCHER_CLOTHES = resLoc("textures/entity/villager/profession/researcher.png")
        private val RESEARCHER_CLOTHES_UNSHEATED_DAGGER = resLoc("textures/entity/villager/profession/researcher_unsheated.png")
    }

    override fun scale(livingEntity: Researcher, matrixStack: PoseStack, partialTickTime: Float) {
        matrixStack.scale(0.9375f, 0.9375f, 0.9375f)
    }
}
