package com.ruslan.growsseth.platform

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier

class PlatformAbstractionsFabric : PlatformAbstractions {
    override fun <T : LivingEntity> registerEntDefaultAttribute(entityType: EntityType<T>, attributeBuilder: AttributeSupplier.Builder) {
        FabricDefaultAttributeRegistry.register(entityType, attributeBuilder)
    }
}