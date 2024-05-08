package com.ruslan.growsseth.platform

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

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
}