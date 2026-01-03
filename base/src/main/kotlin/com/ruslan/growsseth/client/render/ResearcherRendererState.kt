package com.ruslan.growsseth.client.render

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState
import net.minecraft.world.entity.monster.illager.AbstractIllager
import net.minecraft.world.item.ItemStack

class ResearcherRendererState: ArmedEntityRenderState()
{
    // All variables used by the client to determine how to render the Researcher
    var mainHandItem: ItemStack = ItemStack.EMPTY
    var isUsingItem: Boolean = false
    var unhappyCounter: Int = 0
    var isAggressive: Boolean = false
    var armPose: AbstractIllager.IllagerArmPose = AbstractIllager.IllagerArmPose.CROSSED
    var attackAnim: Float = 0f
}