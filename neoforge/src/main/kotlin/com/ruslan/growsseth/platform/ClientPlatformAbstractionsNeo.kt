package com.ruslan.growsseth.platform

import com.ruslan.growsseth.RuinsOfGrowsseth
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
            RuinsOfGrowsseth.LOGGER.debug("Registering renderers")
            queuedRegistrations.forEach { it(event) }
        }
    }

    // Must be called BEFORE the event
    override fun <T : Entity> registerEntityRenderer(
        entityTypeSupplier: () -> EntityType<T>,
        provider: EntityRendererProvider<in T>
    ) {
        queuedRegistrations.add { event ->
            val etype = entityTypeSupplier()
            RuinsOfGrowsseth.LOGGER.debug("Registering renderer for {}", etype)
            event.registerEntityRenderer(etype, provider)
        }
    }
}