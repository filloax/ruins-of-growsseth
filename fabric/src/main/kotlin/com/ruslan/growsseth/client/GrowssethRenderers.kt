package com.ruslan.growsseth.client

import com.ruslan.growsseth.client.render.ResearcherRenderer
import com.ruslan.growsseth.client.render.ZombieResearcherRenderer
import com.ruslan.growsseth.entity.GrowssethEntities
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object GrowssethRenderers {
    private fun <T: Entity> registerRenderer(entityType: EntityType<T>, provider: EntityRendererProvider<in T>) {
        EntityRendererRegistry.register(entityType, provider)
    }

    fun init() {
        registerRenderer(GrowssethEntities.RESEARCHER, ::ResearcherRenderer)
        registerRenderer(GrowssethEntities.ZOMBIE_RESEARCHER, ::ZombieResearcherRenderer)
    }
}