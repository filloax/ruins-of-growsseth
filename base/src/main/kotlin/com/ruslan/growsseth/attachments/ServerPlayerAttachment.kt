package com.ruslan.growsseth.attachments

import com.filloax.fxlib.api.codec.mutableMapCodec
import com.filloax.fxlib.api.platform.ServiceUtil
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.utils.GrowssethCodecs
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.Structure
import java.time.LocalDateTime

// Start using data attachments for server player data, for
// better behavior with mod loaders as they provide it in their
// libraries; things that use filloaxlib player.getPersistData
// persist, but could be gradually ported to this too
// Generic interface, use specific Data Attachment implementation for each loader
data class ServerPlayerAttachment(
    /**
     * Last time the player entered this structure (tag), also includes village houses
     * associated to a structure in [com.ruslan.growsseth.structure.GrowssethStructures.VILLAGE_HOUSE_STRUCTURES]
     */
    val lastStructureEnterTimes: MutableMap<TagKey<Structure>, LocalDateTime> = mutableMapOf(),
) {
    companion object {
        val CREATOR = { ServerPlayerAttachment() }

        val CODEC: Codec<ServerPlayerAttachment> = RecordCodecBuilder.create { builder -> builder.group(
            mutableMapCodec(TagKey.codec(Registries.STRUCTURE), GrowssethCodecs.LOCAL_DATE_TIME_CODEC)
                .optionalFieldOf("lastStructureEnterTimes", mutableMapOf())
                .forGetter(ServerPlayerAttachment::lastStructureEnterTimes)
        ).apply(builder, ::ServerPlayerAttachment) }

        val PROVIDER by lazy { ServiceUtil.findService(ServerPlayerAttachmentProvider::class.java) }
    }
}

fun ServerPlayer.growssethAttachment() = ServerPlayerAttachment.PROVIDER.get(this)

fun interface ServerPlayerAttachmentProvider {
    fun get(player: ServerPlayer): ServerPlayerAttachment
}