package com.ruslan.growsseth.platform

import com.filloax.fxlib.api.platform.ServiceUtil
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

interface ClientPlatformAbstractions {
    fun <T: Entity> registerEntityRenderer(entityType: EntityType<T>, provider: EntityRendererProvider<in T>)

}

val clientPlatform by lazy { ServiceUtil.findService(ClientPlatformAbstractions::class.java) }