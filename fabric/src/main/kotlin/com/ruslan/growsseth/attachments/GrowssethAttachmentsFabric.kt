package com.ruslan.growsseth.attachments

import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.utils.resLoc
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType


@Suppress("UnstableApiUsage")
object GrowssethAttachmentsFabric {
    val SERVER_PLAYER: AttachmentType<ServerPlayerAttachment> = AttachmentRegistry.create(resLoc(Constants.ATTACHMENT_SERVER_PLAYER_DATA)) {
        builder -> builder
            .initializer(ServerPlayerAttachment.CREATOR)
            .persistent(ServerPlayerAttachment.CODEC)
            .copyOnDeath()
    }
}