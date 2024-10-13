package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.api.ScheduledServerTask
import com.filloax.fxlib.api.codec.decodeNbtNullable
import com.filloax.fxlib.api.codec.encodeNbt
import com.filloax.fxlib.api.entity.getPersistData
import com.filloax.fxlib.api.nbt.getListOrNull
import com.filloax.fxlib.api.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.networking.ResearcherTradesNotifPacket
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.trading.MerchantOffers
import kotlin.jvm.optionals.getOrDefault

/**
 * For trade modes that are shared between all researcher entities
 * in the server (allowing for notifications)
 */
abstract class GlobalResearcherTradesProvider protected constructor(
    private val deinitOnServerStop: Boolean = false,
) : AbstractResearcherTradesProvider() {
    protected var loaded = false
        private set
    protected var trades = listOf<ResearcherTradeEntry>()
        private set

    fun init() {
        if (!instances.contains(this))
            instances.add(this)
    }

    companion object {
        private val instances = mutableListOf<GlobalResearcherTradesProvider>()

        fun reloadAll(server: MinecraftServer) {
            instances.forEach { it.reload(server) }
        }
    }

    abstract fun reload(server: MinecraftServer)

    final override fun getOffersImpl(
        researcher: Researcher,
        tradesData: ResearcherTradesData,
        player: ServerPlayer
    ): MerchantOffers {
        val offers = MerchantOffers()
        val trades = getAllTrades() + getExtraPlayerTrades(player, researcher, tradesData)
        offers.addAll(processTrades(trades) // reprocess trades after adding extra player trades
            .filter { isValidTradeForPlayer(it.itemListing, player, researcher, tradesData) }
            .map { it.itemListing.getOffer(researcher, researcher.random) }
        )

        return offers
    }

    fun getAllTrades(): List<ResearcherTradeEntry> {
        return if (loaded) {
            trades
        } else {
            RuinsOfGrowsseth.LOGGER.warn("Tried to load trades for mode $mode before they were properly loaded!")
            listOf()
        }
    }

    protected open fun isValidTradeForPlayer(trade: ResearcherItemListing, player: ServerPlayer, researcher: Researcher, data: ResearcherTradesData): Boolean {
        return true
    }

    protected open fun getExtraPlayerTrades(player: ServerPlayer, researcher: Researcher, data: ResearcherTradesData): List<ResearcherTradeEntry> {
        return listOf()
    }

    /**
     * Shall be called after the child class generates its new global trade list
     */
    protected fun applyUpdatedTrades(server: MinecraftServer, newTrades: List<ResearcherTradeEntry>) {
        val processedTrades = processTrades(newTrades)
        trades = processedTrades
        loaded = true

        val savedTrades = GlobalTradesSavedData.getGlobalTrades(server)

        val addedTrades = trades.filter { savedTrades.none{ it2 -> it.looselyMatches(it2) } }
        val removedTrades = savedTrades.filter { trades.none{ it2 -> it.looselyMatches(it2) } }

        if (addedTrades.isNotEmpty()) {
            onNewTrades(server, addedTrades.map{it.itemListing})
        }

        savedTrades.clear()
        savedTrades.addAll(trades)
        GlobalTradesSavedData.setDirty(server)

        RuinsOfGrowsseth.LOGGER.info("Updated global researcher trades! Now has ${trades.size} (${addedTrades.size} new, ${removedTrades.size} removed)")
    }

    protected open fun onNewTrades(server: MinecraftServer, newTrades: List<ResearcherItemListing>) {
        if (!isEnabled(server)) return

        val dataList: Tag = ResearcherTradeEntry.LIST_CODEC.encodeNbt(GameMasterResearcherTradesProvider.trades).result().getOrDefault(ListTag())
        server.playerList.players.forEach { player ->
            val metResearcher = player.getPersistData().getBoolean(Constants.DATA_PLAYER_MET_RESEARCHER)
            if (metResearcher) notifyPlayer(player, newTrades, {
                val data = player.getPersistData()
                data.put("ResearcherTradeMemory", dataList)
            })
        }
    }

    protected fun notifyPlayer(player: ServerPlayer, newTrades: List<ResearcherItemListing>, after: () -> Unit = {}) {
        val notifiableNewTrades = newTrades.filterNot { it.noNotification }
        if (notifiableNewTrades.isEmpty()) return

        // TODO: non-fabric specific sendPacket
//        sender.sendPacket(ResearcherTradesNotifPacket(notifiableNewTrades), object : PacketSendListener {
//            override fun onSuccess() = after()
//        })
    }

    private fun onServerStop(server: MinecraftServer) {
        if (server.overworld() != null) {
            val savedTrades = GlobalTradesSavedData.getGlobalTrades(server)
            savedTrades.clear()
            savedTrades.addAll(trades)
            GlobalTradesSavedData.setDirty(server)
        }

        // clear trades list
        trades = listOf()
        loaded = false
    }

    private fun onServerPlayerJoin(handler: ServerGamePacketListenerImpl, server: MinecraftServer) {
        if (!isEnabled(server)) {
            return
        }

        val player = handler.player
        val data = player.getPersistData()
        val metResearcher = player.getPersistData().getBoolean(Constants.DATA_PLAYER_MET_RESEARCHER)
        val itemListingTrades by lazy { trades.map{ it.itemListing } }
        val dataList by lazy { ResearcherItemListing.LIST_CODEC.encodeNbt(itemListingTrades).resultOrPartial().getOrDefault(ListTag()) }
        if (metResearcher)
            data.getListOrNull("ResearcherTradeMemory", Tag.TAG_COMPOUND)?.let { dataListKnown ->
                val savedTrades = ResearcherItemListing.LIST_CODEC.decodeNbtNullable(dataListKnown) ?: listOf()
                val newTrades = itemListingTrades.filter { savedTrades.none{ it2 -> it.looselyMatches(it2) } }

                if (newTrades.isNotEmpty()) {
                    RuinsOfGrowsseth.LOGGER.info("Sending trade notification to player on login (has ${newTrades.size} new)")
                    ScheduledServerTask.schedule(server, 40) {
                        notifyPlayer(player, newTrades, after={
                            data.put("ResearcherTradeMemory", dataList)
                        })
                    }
                }
            } ?: run {
                data.put("ResearcherTradeMemory", dataList)
            }
    }

    // Save data for all researcher trades directly in world save, to make notifying for new ones easier
    // Shared betweem all subclasses, which has the side effect of showing notifications even on
    // trading mode change which might actually be desired
    // (Commented out is a version with separate data for modes in case that becomes more desirable)
    private class GlobalTradesSavedData(
        val trades: MutableList<ResearcherTradeEntry> = mutableListOf(),
//        val trades: Map<ResearcherTradeMode, MutableList<ResearcherItemListing>> =
//            ResearcherTradeMode.entries.toTypedArray().associateWith { mutableListOf() }
    ) : FxSavedData<GlobalTradesSavedData>(CODEC) {
        companion object {
            val CODEC: Codec<GlobalTradesSavedData> = RecordCodecBuilder.create { builder -> builder.group(
                ResearcherTradeEntry.MLIST_CODEC.fieldOf("trades").forGetter(GlobalTradesSavedData::trades),
//                Codec.unboundedMap(ResearcherTradeMode.CODEC, ResearcherItemListing.MLIST_CODEC)
//                    .fieldOf("trades").forGetter(GlobalTradesSavedData::trades)
            ).apply(builder, ::GlobalTradesSavedData) }

            val DEF = define("GlobalTrades", ::GlobalTradesSavedData, CODEC)

            fun getGlobalTrades(server: MinecraftServer) = server.loadData(DEF).trades
            fun setDirty(server: MinecraftServer) = server.loadData(DEF).setDirty()
        }
    }

    object Callbacks {
        fun onServerStop(server: MinecraftServer) {
            instances.forEach { it.onServerStop(server) }
            instances.removeIf { it.deinitOnServerStop }
        }

        fun onServerPlayerJoin(handler: ServerGamePacketListenerImpl, server: MinecraftServer) {
            instances.forEach { it.onServerPlayerJoin(handler, server) }
        }
    }
}