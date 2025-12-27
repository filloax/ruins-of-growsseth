package com.ruslan.growsseth.client.render

import com.ruslan.growsseth.entity.researcher.Researcher
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState

class ResearcherRendererState: ArmedEntityRenderState()
{
    // todo: change class to not take the entity directly but just what it needs
    var entity: Researcher? = null
    var attackAnim: Float = 0f
}