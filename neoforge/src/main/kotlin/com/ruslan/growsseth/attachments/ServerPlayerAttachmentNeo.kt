package com.ruslan.growsseth.attachments

import net.minecraft.server.level.ServerPlayer

class ServerPlayerAttachmentProviderNeo : ServerPlayerAttachmentProvider {
    override fun get(player: ServerPlayer): ServerPlayerAttachment {
        return player.getData(GrowssethAttachmentsNeo.SERVER_PLAYER.get())
    }
}