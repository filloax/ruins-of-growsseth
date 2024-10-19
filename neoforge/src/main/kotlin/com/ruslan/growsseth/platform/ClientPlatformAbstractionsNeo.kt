package com.ruslan.growsseth.platform

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.EntityRenderersEvent

class ClientPlatformAbstractionsNeo : ClientPlatformAbstractions {
    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    companion object {
        private val queuedRegistrations = mutableListOf<(EntityRenderersEvent.RegisterRenderers) -> Unit>()

        @SubscribeEvent
        fun registerAttributes(event: EntityRenderersEvent.RegisterRenderers) {
            queuedRegistrations.forEach { it(event) }
        }
    }

    // Must be called BEFORE the event
    override fun <T : Entity> registerEntityRenderer(
        entityType: EntityType<T>,
        provider: EntityRendererProvider<in T>
    ) {
        queuedRegistrations.add { event ->
            event.registerEntityRenderer(entityType, provider)
        }
    }
}