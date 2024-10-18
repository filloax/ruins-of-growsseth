package com.ruslan.growsseth.network

import com.filloax.fxlib.api.FxLibServices
import com.filloax.fxlib.api.codec.streamCodec
import com.filloax.fxlib.api.optional
import com.filloax.fxlib.api.networking.playS2C
import com.mojang.serialization.Codec
import com.ruslan.growsseth.dialogues.DialogueLine
import com.ruslan.growsseth.entity.researcher.trades.ResearcherItemListing
import com.ruslan.growsseth.platform.PlatformAbstractions
import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.worldpreset.LocationData
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.TypeAndCodec
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

typealias RStreamCodec<T> = StreamCodec<RegistryFriendlyByteBuf, T>

object GrowssethPackets {
    object Types {
        val TRADE_NOTIF = resLoc("researcher_trades_notif")
        val CUSTOM_TOAST = resLoc("custom_toast")
        val STOP_MUSIC = resLoc("stop_music")
        val FORCE_AMBIENT_SOUND = resLoc("force_ambient_sound")
        val PLACES_DATA = resLoc("places_data")
        val DIALOGUE = resLoc("dialogue")
    }

    val DIALOGUE            = DialoguePacket.ENTRY
    val TRADE_NOTIF         = ResearcherTradesNotifPacket.ENTRY
    val CUSTOM_TOAST        = CustomToastPacket.ENTRY
    val STOP_MUSIC          = StopMusicPacket.ENTRY
    val FORCE_AMBIENT_SOUND = AmbientSoundsPacket.ENTRY
    val PLACES_DATA         = PlacesInfoPacket.ENTRY

    fun registerPacketsS2C() {
        FxLibServices.networking.packetRegistrator.apply {
            playS2C(DIALOGUE, ClientPacketHandlers::handleDialogue)
            playS2C(TRADE_NOTIF, ClientPacketHandlers::handleTradeNotification)
            playS2C(CUSTOM_TOAST, ClientPacketHandlers::handleCustomToast)
            playS2C(STOP_MUSIC, ClientPacketHandlers::handleStopMusic)
            playS2C(FORCE_AMBIENT_SOUND, ClientPacketHandlers::handleAmbientSounds)
            playS2C(PLACES_DATA, ClientPacketHandlers::handlePlacesInfo)
        }
    }

    fun registerPacketsC2S() {
    }
}

data class DialoguePacket(
    val dialogueLine: DialogueLine,
    val senderName: Component,
) : CustomPacketPayload  {
    companion object {
        val TYPE = CustomPacketPayload.Type<DialoguePacket>(GrowssethPackets.Types.DIALOGUE)
        val CODEC: RStreamCodec<DialoguePacket> = StreamCodec.composite(
            DialogueLine.serializer().streamCodec(), DialoguePacket::dialogueLine,
            ComponentSerialization.STREAM_CODEC, DialoguePacket::senderName,
            ::DialoguePacket
        )
        val ENTRY = TypeAndCodec(TYPE, CODEC)

        private fun write(buf: RegistryFriendlyByteBuf, packet: ResearcherTradesNotifPacket) = buf.writeJsonWithCodec(ResearcherItemListing.LIST_CODEC, packet.newTrades)
        private fun read(buf: RegistryFriendlyByteBuf) = ResearcherTradesNotifPacket(buf.readJsonWithCodec(ResearcherItemListing.LIST_CODEC))
    }

    override fun type(): CustomPacketPayload.Type<DialoguePacket> = TYPE
}

data class ResearcherTradesNotifPacket(
    val newTrades: List<ResearcherItemListing>,
) : CustomPacketPayload  {
    companion object {
        val TYPE = CustomPacketPayload.Type<ResearcherTradesNotifPacket>(GrowssethPackets.Types.TRADE_NOTIF)
        val CODEC: RStreamCodec<ResearcherTradesNotifPacket> = StreamCodec.of(::write, ::read)
        val ENTRY = TypeAndCodec(TYPE, CODEC)

        private fun write(buf: RegistryFriendlyByteBuf, packet: ResearcherTradesNotifPacket) = buf.writeJsonWithCodec(ResearcherItemListing.LIST_CODEC, packet.newTrades)
        private fun read(buf: RegistryFriendlyByteBuf) = ResearcherTradesNotifPacket(buf.readJsonWithCodec(ResearcherItemListing.LIST_CODEC))
    }

    override fun type(): CustomPacketPayload.Type<ResearcherTradesNotifPacket> = TYPE
}

open class CustomToastPacket(
    val title: Component,
    val message: Component? = null,
    val item: ItemStack = ItemStack.EMPTY,
) : CustomPacketPayload {
    companion object {
        val CODEC: RStreamCodec<CustomToastPacket> = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, CustomToastPacket::title,
            ComponentSerialization.OPTIONAL_STREAM_CODEC, CustomToastPacket::message.optional(),
            ItemStack.OPTIONAL_STREAM_CODEC, CustomToastPacket::item,
        ) { title, message, item -> CustomToastPacket(title, message.getOrNull(), item) }

        val TYPE = CustomPacketPayload.Type<CustomToastPacket>(GrowssethPackets.Types.CUSTOM_TOAST)
        val ENTRY = TypeAndCodec(TYPE, CODEC)

    }

    override fun type() = TYPE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomToastPacket

        if (title != other.title) return false
        return message == other.message
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "CustomToastPacket(title=$title, message=$message, item=$item)"
    }
}

data class PlacesInfoPacket(
    val locationData: List<LocationData>
) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(buf.readJsonWithCodec(Codec.list(LocationData.CODEC)))

    companion object {
        val CODEC: RStreamCodec<PlacesInfoPacket> = StreamCodec.of(::write, ::read)
        val TYPE = CustomPacketPayload.Type<PlacesInfoPacket>(GrowssethPackets.Types.PLACES_DATA)
        val ENTRY = TypeAndCodec(TYPE, CODEC)

        private fun write(buf: RegistryFriendlyByteBuf, pak: PlacesInfoPacket) = buf.writeJsonWithCodec(LocationData.CODEC.listOf(), pak.locationData)
        private fun read(buf: RegistryFriendlyByteBuf) = PlacesInfoPacket(buf.readJsonWithCodec(LocationData.CODEC.listOf()))
    }

    override fun type() = TYPE
}

abstract class EmptyPacket : CustomPacketPayload {
    companion object {
        fun <T : EmptyPacket> codec(cons: () -> T): RStreamCodec<T> = StreamCodec.of({ _, _ ->}, { cons() })
    }
}

class StopMusicPacket : EmptyPacket() {
    companion object {
        val TYPE = CustomPacketPayload.Type<StopMusicPacket>(GrowssethPackets.Types.STOP_MUSIC)
        val CODEC = codec(::StopMusicPacket)
        val ENTRY = TypeAndCodec(TYPE, CODEC)
    }

    override fun type() = TYPE
}

class AmbientSoundsPacket : EmptyPacket() {
    companion object {
        val TYPE = CustomPacketPayload.Type<AmbientSoundsPacket>(GrowssethPackets.Types.FORCE_AMBIENT_SOUND)
        val CODEC = codec(::AmbientSoundsPacket)
        val ENTRY = TypeAndCodec(TYPE, CODEC)
    }

    override fun type() = TYPE
}