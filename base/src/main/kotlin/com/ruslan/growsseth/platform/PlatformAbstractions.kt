package com.ruslan.growsseth.platform

import com.filloax.fxlib.api.platform.ServiceUtil
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.TypeAndCodec
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier

interface PlatformAbstractions {
    fun <T : LivingEntity>  registerEntDefaultAttribute(entityType: EntityType<T>, attributeBuilder: AttributeSupplier.Builder)
}

val platform: PlatformAbstractions by lazy { ServiceUtil.findService(PlatformAbstractions::class.java) }