package com.ruslan.growsseth.platform

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent

class PlatformAbstractionsNeo : PlatformAbstractions {
    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    companion object {
        private val queuedRegistrations = mutableMapOf<EntityType<out LivingEntity>, AttributeSupplier>()

        @SubscribeEvent
        fun registerAttributes(event: EntityAttributeCreationEvent) {
            queuedRegistrations.forEach { (t, a) -> event.put(t, a) }
        }
    }

    // Should be called in init BEFORE the event
    override fun <T : LivingEntity> registerEntDefaultAttribute(
        entityType: EntityType<T>,
        attributeBuilder: AttributeSupplier.Builder
    ) {
        queuedRegistrations[entityType] = attributeBuilder.build()
    }
}