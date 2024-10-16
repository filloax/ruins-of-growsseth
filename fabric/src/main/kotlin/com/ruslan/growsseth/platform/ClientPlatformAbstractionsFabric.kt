package com.ruslan.growsseth.platform

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

class ClientPlatformAbstractionsFabric : ClientPlatformAbstractions {
    override fun <T : Entity> registerEntityRenderer(
        entityType: EntityType<T>,
        provider: EntityRendererProvider<in T>
    ) {
        EntityRendererRegistry.register(entityType, provider)
    }
}