package com.ruslan.growsseth.attachments

import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries


object GrowssethAttachmentsNeo {
    private val ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RuinsOfGrowsseth.MOD_ID)

    // Serialization via INBTSerializable
    val SERVER_PLAYER: DeferredHolder<AttachmentType<*>, AttachmentType<ServerPlayerAttachment>> =
        ATTACHMENT_TYPES.register(Constants.ATTACHMENT_SERVER_PLAYER_DATA) { _ ->
            AttachmentType.builder(ServerPlayerAttachment.CREATOR)
                .serialize(ServerPlayerAttachment.CODEC.fieldOf(Constants.ATTACHMENT_SERVER_PLAYER_DATA))
                .build()
        }

    fun registerAll(bus: IEventBus) {
        ATTACHMENT_TYPES.register(bus)
    }
}