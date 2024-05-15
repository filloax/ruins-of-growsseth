package com.ruslan.growsseth.platform

import com.filloax.fxlib.api.platform.ServiceUtil
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.TypeAndCodec

interface PlatformAbstractions {

    companion object {
        fun get(): PlatformAbstractions = ServiceUtil.findService(PlatformAbstractions::class.java)
    }
}