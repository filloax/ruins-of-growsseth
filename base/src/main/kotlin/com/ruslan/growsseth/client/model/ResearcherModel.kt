package com.ruslan.growsseth.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.ruslan.growsseth.entity.researcher.Researcher
import net.minecraft.client.model.AnimationUtils
import net.minecraft.client.model.ArmedModel
import net.minecraft.client.model.HeadedModel
import net.minecraft.client.model.HierarchicalModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.monster.AbstractIllager.IllagerArmPose

// Merges villagers and illagers stuff to render him both ways
class ResearcherModel(private val root: ModelPart) : HierarchicalModel<Researcher>(), ArmedModel, HeadedModel {
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

    override fun root(): ModelPart {
        return this.root
    }

    /**
     * Sets this entity's model rotation angles
     */
    override fun setupAnim(
        entity: Researcher,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        head.yRot = netHeadYaw * (Math.PI / 180.0).toFloat()
        head.xRot = headPitch * (Math.PI / 180.0).toFloat()

        // Taken from VillagerModel for the head shaking animation when refusing to trade (except when fighting)
        val isUnhappy = (entity.unhappyCounter > 0 && !entity.isAggressive)
        if (isUnhappy) {
            head.zRot = 0.3f * Mth.sin(0.45f * ageInTicks)
            head.xRot = 0.4f
        } else
            head.zRot = 0.0f

        rightArm.xRot = Mth.cos(limbSwing * 0.6662f + Math.PI.toFloat()) * 2.0f * limbSwingAmount * 0.5f
        rightArm.yRot = 0.0f
        rightArm.zRot = 0.0f
        leftArm.xRot = Mth.cos(limbSwing * 0.6662f) * 2.0f * limbSwingAmount * 0.5f
        leftArm.yRot = 0.0f
        leftArm.zRot = 0.0f

        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount * 0.5f
        rightLeg.yRot = 0.0f
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662f + Math.PI.toFloat()) * 1.4f * limbSwingAmount * 0.5f
        leftLeg.yRot = 0.0f

        val illagerArmPose = entity.armPose

        val bl = illagerArmPose == IllagerArmPose.CROSSED
        arms.visible = bl
        leftArm.visible = !bl
        rightArm.visible = !bl

        if (illagerArmPose == IllagerArmPose.ATTACKING) {
            if (entity.isAggressive) {
                if (entity.mainHandItem.isEmpty) {
                    AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, this.attackTime, ageInTicks)
                }
                else {
                    if (!entity.isUsingItem)
                        AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, entity, this.attackTime, ageInTicks)
                    else
                        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, this.attackTime, ageInTicks)
                }
            }
            else if (entity.isUsingItem){
                AnimationUtils.swingWeaponDown(this.leftArm, this.rightArm, entity, this.attackTime, ageInTicks)
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