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
    override val packetRegistratorC2S = object : PlatformAbstractions.PacketRegistrator<RegistryFriendlyByteBuf> {
        val payloadRegistry = PayloadTypeRegistry.playC2S()
        override fun <T : CustomPacketPayload> register(
            id: CustomPacketPayload.Type<T>,
            codec: StreamCodec<in RegistryFriendlyByteBuf, T>
        ): CustomPacketPayload.TypeAndCodec<in RegistryFriendlyByteBuf, T> = payloadRegistry.register(id, codec)
    }

    override val packetRegistratorS2C = object : PlatformAbstractions.PacketRegistrator<RegistryFriendlyByteBuf> {
        val payloadRegistry = PayloadTypeRegistry.playS2C()
        override fun <T : CustomPacketPayload> register(
            id: CustomPacketPayload.Type<T>,
            codec: StreamCodec<in RegistryFriendlyByteBuf, T>
        ): CustomPacketPayload.TypeAndCodec<in RegistryFriendlyByteBuf, T> = payloadRegistry.register(id, codec)
    }

    override fun <T : LivingEntity> registerEntDefaultAttribute(entityType: EntityType<T>, attributeBuilder: AttributeSupplier.Builder) {
        FabricDefaultAttributeRegistry.register(entityType, attributeBuilder)
    }
}