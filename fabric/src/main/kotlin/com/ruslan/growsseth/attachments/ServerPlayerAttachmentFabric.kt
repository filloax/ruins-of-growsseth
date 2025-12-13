package com.ruslan.growsseth.attachments

import net.minecraft.server.level.ServerPlayer

@Suppress("UnstableApiUsage")
class ServerPlayerAttachmentProviderFabric : ServerPlayerAttachmentProvider {
    override fun get(player: ServerPlayer): ServerPlayerAttachment {
        return player.getAttachedOrCreate(GrowssethAttachmentsFabric.SERVER_PLAYER)
    }
}