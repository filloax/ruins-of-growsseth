package com.ruslan.growsseth.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.ruslan.growsseth.client.render.ResearcherRendererState
import com.ruslan.growsseth.entity.researcher.Researcher
import net.minecraft.client.model.AnimationUtils
import net.minecraft.client.model.ArmedModel
import net.minecraft.client.model.HeadedModel
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.monster.AbstractIllager.IllagerArmPose

// Merges villagers and illagers stuff to render him both ways
class ResearcherModel(private val root: ModelPart) : EntityModel<ResearcherRendererState>(root), ArmedModel, HeadedModel {
    private val head: ModelPart = root.getChild("head")
    val hat: ModelPart = head.getChild("hat")
    private val arms: ModelPart
    private val leftLeg: ModelPart
    private val rightLeg: ModelPart
    private val rightArm: ModelPart
    private val leftArm: ModelPart

    init {
        hat.visible = false
        this.arms = root.getChild("arms")
        this.leftLeg = root.getChild("left_leg")
        this.rightLeg = root.getChild("right_leg")
        this.leftArm = root.getChild("left_arm")
        this.rightArm = root.getChild("right_arm")
    }

    /**
     * Sets this entity's model rotation angles
     */
    override fun setupAnim(renderState: ResearcherRendererState) {
        head.yRot = renderState.yRot * (Math.PI / 180.0).toFloat()
        head.xRot = renderState.xRot * (Math.PI / 180.0).toFloat()

        // Taken from VillagerModel for the head shaking animation when refusing to trade (except when fighting)
        val isUnhappy = (renderState.entity!!.unhappyCounter > 0 && !renderState.entity!!.isAggressive)
        if (isUnhappy) {
            head.zRot = 0.3f * Mth.sin(0.45f * renderState.ageInTicks)
            head.xRot = 0.4f
        } else
            head.zRot = 0.0f

        // todo
//        rightArm.xRot = Mth.cos(limbSwing * 0.6662f + Math.PI.toFloat()) * 2.0f * limbSwingAmount * 0.5f
//        rightArm.yRot = 0.0f
//        rightArm.zRot = 0.0f
//        leftArm.xRot = Mth.cos(limbSwing * 0.6662f) * 2.0f * limbSwingAmount * 0.5f
//        leftArm.yRot = 0.0f
//        leftArm.zRot = 0.0f
//
//        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount * 0.5f
//        rightLeg.yRot = 0.0f
//        leftLeg.xRot = Mth.cos(limbSwing * 0.6662f + Math.PI.toFloat()) * 1.4f * limbSwingAmount * 0.5f
//        leftLeg.yRot = 0.0f

        val illagerArmPose = renderState.entity!!.armPose

        val bl = illagerArmPose == IllagerArmPose.CROSSED
        arms.visible = bl
        leftArm.visible = !bl
        rightArm.visible = !bl

        if (illagerArmPose == IllagerArmPose.ATTACKING) {
            if (renderState.entity!!.isAggressive) {
                if (renderState.entity!!.mainHandItem.isEmpty) {
                    AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, renderState.attackAnim, renderState.ageInTicks)
                }
                else {
                    if (!renderState.entity!!.isUsingItem)
                        AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, renderState.entity!!.mainArm, renderState.attackAnim, renderState.ageInTicks)
                    else
                        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, renderState.attackAnim, renderState.ageInTicks)
                }
            }
            else if (renderState.entity!!.isUsingItem){
                AnimationUtils.swingWeaponDown(this.leftArm, this.rightArm, renderState.entity!!.mainArm, renderState.attackAnim, renderState.ageInTicks)
            }
        }
    }

    private fun getArm(arm: HumanoidArm): ModelPart {
        return if (arm == HumanoidArm.LEFT) this.leftArm else this.rightArm
    }

    override fun getHead(): ModelPart {
        return this.head
    }

    override fun translateToHand(side: HumanoidArm, poseStack: PoseStack) {
        getArm(side).translateAndRotate(poseStack)
    }
}