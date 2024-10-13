package com.ruslan.growsseth.client

import com.ruslan.growsseth.client.render.ResearcherRenderer
import com.ruslan.growsseth.client.render.ZombieResearcherRenderer
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.platform.clientPlatform

object GrowssethRenderers {
    fun init() {
        clientPlatform.registerEntityRenderer(GrowssethEntities.RESEARCHER, ::ResearcherRenderer)
        clientPlatform.registerEntityRenderer(GrowssethEntities.ZOMBIE_RESEARCHER, ::ZombieResearcherRenderer)
    }
}