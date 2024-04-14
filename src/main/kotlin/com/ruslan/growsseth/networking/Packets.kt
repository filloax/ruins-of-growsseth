package com.ruslan.growsseth.networking

import com.mojang.serialization.Codec
import com.ruslan.growsseth.entity.researcher.trades.ResearcherItemListing
import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.worldpreset.LocationData
import net.fabricmc.fabric.api.networking.v1.FabricPacket
import net.fabricmc.fabric.api.networking.v1.PacketType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

object GrowssethPackets {
    val TRADE_NOTIF = resLoc("researcher_trades_notif")
    val CUSTOM_TOAST = resLoc("custom_toast")
    val STOP_MUSIC = resLoc("stop_music")
    val FORCE_AMBIENT_SOUND = resLoc("force_ambient_sound")
    val PLACES_DATA = resLoc("places_data")
}

data class ResearcherTradesNotifPacket(
    val newTrades: List<ResearcherItemListing>,
) : FabricPacket {
    constructor(buf: FriendlyByteBuf) : this(buf.readJsonWithCodec(ResearcherItemListing.LIST_CODEC))

    companion object {
        val TYPE: PacketType<ResearcherTradesNotifPacket> = PacketType.create(GrowssethPackets.TRADE_NOTIF, ::ResearcherTradesNotifPacket)
    }

    override fun getType(): PacketType<*> = TYPE

    override fun write(buf: FriendlyByteBuf) {
        buf.writeJsonWithCodec(ResearcherItemListing.LIST_CODEC, newTrades)
    }
}

open class CustomToastPacket(
    val title: Component,
    val message: Component? = null,
    val item: ItemStack? = null,
) : FabricPacket {
    constructor(buf: FriendlyByteBuf) : this(
        buf.readComponent(),
        if (buf.readBoolean()) buf.readComponent() else null,
        if (buf.readBoolean()) buf.readItem() else null,
    )

    override fun write(buf: FriendlyByteBuf) {
        buf.writeComponent(title)
        message?.let {
            buf.writeBoolean(true)
            buf.writeComponent(it)
        } ?: buf.writeBoolean(false)
        item?.let {
            buf.writeBoolean(true)
            buf.writeItem(it)
        } ?: buf.writeBoolean(false)
    }

    companion object {
        val TYPE: PacketType<CustomToastPacket> = PacketType.create(GrowssethPackets.CUSTOM_TOAST, ::CustomToastPacket)
    }

    override fun getType(): PacketType<*> = TYPE


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
) : FabricPacket {
    constructor(buf: FriendlyByteBuf) : this(buf.readJsonWithCodec(Codec.list(LocationData.CODEC)))

    companion object {
        val TYPE: PacketType<PlacesInfoPacket> = PacketType.create(GrowssethPackets.PLACES_DATA, ::PlacesInfoPacket)
    }

    override fun getType(): PacketType<*> = TYPE

    override fun write(buf: FriendlyByteBuf) {
        buf.writeJsonWithCodec(Codec.list(LocationData.CODEC), locationData)
    }

}

abstract class EmptyPacket() : FabricPacket {
    override fun write(buf: FriendlyByteBuf) {}
}

class StopMusicPacket() : EmptyPacket() {
    constructor(buf: FriendlyByteBuf) : this()

    companion object {
        val TYPE: PacketType<StopMusicPacket> = PacketType.create(GrowssethPackets.STOP_MUSIC, ::StopMusicPacket)
    }

    override fun getType(): PacketType<*> = TYPE
}

class AmbientSoundsPacket() : EmptyPacket() {
    constructor(buf: FriendlyByteBuf) : this()

    companion object {
        val TYPE: PacketType<AmbientSoundsPacket> = PacketType.create(GrowssethPackets.FORCE_AMBIENT_SOUND, ::AmbientSoundsPacket)
    }

    override fun getType(): PacketType<*> = TYPE
}