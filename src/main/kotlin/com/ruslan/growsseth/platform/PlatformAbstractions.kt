package com.ruslan.growsseth.platform

import com.filloax.fxlib.platform.ServiceUtil
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.TypeAndCodec

interface PlatformAbstractions {
    val packetRegistratorC2S: PacketRegistrator<RegistryFriendlyByteBuf>
    val packetRegistratorS2C: PacketRegistrator<RegistryFriendlyByteBuf>

    companion object {
        fun get(): PlatformAbstractions = ServiceUtil.findService(PlatformAbstractions::class.java)
    }


    // Based on Fabric API
    interface PacketRegistrator<B : FriendlyByteBuf> {
        fun <T : CustomPacketPayload> register(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in B, T>): TypeAndCodec<in B, T>
        fun <T : CustomPacketPayload> register(entry: TypeAndCodec<in B, T>): TypeAndCodec<in B, T> = register(entry.type(), entry.codec())
    }
}