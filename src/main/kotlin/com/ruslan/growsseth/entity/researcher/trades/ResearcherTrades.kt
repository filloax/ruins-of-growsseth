package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.codec.*
import com.filloax.fxlib.*
import com.filloax.fxlib.entity.getPersistData
import com.filloax.fxlib.nbt.getListOrNull
import com.filloax.fxlib.nbt.loadField
import com.filloax.fxlib.nbt.saveField
import com.filloax.fxlib.structure.FixedStructureGeneration
import com.mojang.datafixers.util.Either
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.StructureAdvancements
import com.ruslan.growsseth.config.GrowssethConfig
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.http.ApiEvent
import com.ruslan.growsseth.http.ApiQuestData
import com.ruslan.growsseth.http.GrowssethApi
import com.ruslan.growsseth.maps.DestinationType
import com.ruslan.growsseth.maps.updateMapToPos
import com.ruslan.growsseth.maps.updateMapToStruct
import com.ruslan.growsseth.mixin.entity.EntityAccessor
import com.ruslan.growsseth.mixin.item.mapitem.MapItemAccessor
import com.ruslan.growsseth.networking.ResearcherTradesNotifPacket
import com.ruslan.growsseth.structure.RemoteStructures
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.level.saveddata.SavedData

object ResearcherTrades {
    private var loaded = false
    private var trades = listOf<ResearcherItemListing>()
    private val JSON = Json { isLenient = true }
    private val fixedStructureGeneration = FxLibServices.fixedStructureGeneration

    fun init() {
        GrowssethApi.current.subscribe { api, server ->
            EventUtil.runWhenServerStarted(server) { server2 ->
                updateTrades(server2, api)
                loaded = true
            }
        }
    }

    private fun updateTrades(server: MinecraftServer, api: GrowssethApi = GrowssethApi.current) {
        val workingList = mutableListOf<ResearcherTradeEntry>()
        workingList.addAll(TradesListener.FIXED_TRADES)
        workingList.addAll(getUnlockedTradesByStruct())
        workingList.addAll(getUnlockedTradesByQuest(api.quests))
        workingList.addAll(getUnlockedTradesByEvent(api.events))
        workingList.addAll(getCustomTrades(api.events))
        workingList.addAll(getSimpleEventTrades(api.events))
        filterReplaces(workingList)
        workingList.sortBy { it.priority }

        trades = workingList.map(ResearcherTradeEntry::itemListing)

        val savedTrades = TradesSavedData.get(server)

        val newTrades = trades.filter { savedTrades.none{ it2 -> sameTrade(it, it2) } }
        val removedTrades = savedTrades.filter { trades.none{ it2 -> sameTrade(it, it2) } }

        if (newTrades.isNotEmpty()) {
            onNewTrades(server, newTrades)
        }

        savedTrades.clear()
        savedTrades.addAll(trades)
        TradesSavedData.setDirty(server)

        RuinsOfGrowsseth.LOGGER.info("Updated global researcher trades! Now has ${trades.size} (${newTrades.size} new, ${removedTrades.size} removed)")
    }

    /** Apply the replaces parameter, on-place */
    private fun filterReplaces(list: MutableList<ResearcherTradeEntry>) {
        // Make a list in case no trade with that item has replaces, and so all are kept
        val valueToItemMap = mutableMapOf<Item, MutableList<ResearcherTradeEntry>>()

        for (entry in list) {
            val value = entry.itemListing.gives.item
            val itemList = valueToItemMap.computeIfAbsent(value) { mutableListOf() }
            if (entry.replace) {
                // Priority affects order such that low priority number goes first, so treat it the same here
                itemList.removeAll { it.priority > entry.priority || !it.replace }
            }
            itemList.add(entry)
        }

        list.clear()
        list.addAll(valueToItemMap.values.flatten())
    }

    fun onServerStop(server: MinecraftServer) {
        if (server.overworld() != null) {
            val savedTrades = TradesSavedData.get(server)
            savedTrades.clear()
            savedTrades.addAll(trades)
            TradesSavedData.setDirty(server)
        }

        // clear trades list
        trades = listOf()
        loaded = false
    }

    private fun sameTrade(listing1: ResearcherItemListing, listing2: ResearcherItemListing): Boolean {
        // Same output
        return ItemStack.matches(listing1.gives, listing2.gives) && listing1.mapInfo == listing2.mapInfo
    }

    private fun onNewTrades(server: MinecraftServer, newTrades: List<ResearcherItemListing>) {
        val dataList: Tag = ResearcherItemListing.LIST_CODEC.encodeNbt(trades).get().map({it}, { ListTag() })
        server.playerList.players.forEach { player ->
            val metResearcher = player.getPersistData().getBoolean(Constants.DATA_PLAYER_MET_RESEARCHER)
            if (metResearcher) notifyPlayer(player, newTrades, {
                val data = player.getPersistData()
                data.put("ResearcherTradeMemory", dataList)
            })
        }
    }

    private fun notifyPlayer(player: ServerPlayer, newTrades: List<ResearcherItemListing>, after: () -> Unit, sender: PacketSender = ServerPlayNetworking.getSender(player)) {
        val notifiableNewTrades = newTrades.filterNot { it.noNotification }
        if (notifiableNewTrades.isEmpty()) return

        sender.sendPacket(ResearcherTradesNotifPacket(notifiableNewTrades)) { after() }
    }

    fun onServerPlayerJoin(handler: ServerGamePacketListenerImpl, sender: PacketSender, server: MinecraftServer) {
        val player = handler.player
        val data = player.getPersistData()
        val metResearcher =player.getPersistData().getBoolean(Constants.DATA_PLAYER_MET_RESEARCHER)
        val dataList: Tag = ResearcherItemListing.LIST_CODEC.encodeNbt(trades).get().map({it}, { ListTag() })
        if (metResearcher) data.getListOrNull("ResearcherTradeMemory", Tag.TAG_COMPOUND)?.let { dataListKnown ->
            val savedTrades: List<ResearcherItemListing> = ResearcherItemListing.LIST_CODEC.decodeNbt(dataListKnown).get().map({it.first}, { listOf() })
            val newTrades = trades.filter { savedTrades.none{ it2 -> sameTrade(it, it2) } }

            if (newTrades.isNotEmpty()) {
                RuinsOfGrowsseth.LOGGER.info("Sending trade notification to player on login (has ${newTrades.size} new)")
                ScheduledServerTask.schedule(server, 40) {
                    notifyPlayer(player, newTrades, sender=sender, after={
                        data.put("ResearcherTradeMemory", dataList)
                    })
                }
            }
        } else {
            data.put("ResearcherTradeMemory", dataList)
        }
    }

    @JvmStatic
    fun getOffers(entity: Researcher): MerchantOffers {
        val offers = MerchantOffers()
        val player = entity.offersPlayer
        offers.addAll(
            getAllTrades()
                .filter { player?.let{ p -> isValidTradeForPlayer(it, p, entity)} ?: run {
                    RuinsOfGrowsseth.LOGGER.warn("Player null when getting researcher trades")
                    true
                }  }
                .map { it.getOffer(entity, (entity as EntityAccessor).random) }
        )
        return offers
    }

    private fun isValidTradeForPlayer(trade: ResearcherItemListing, player: Player, entity: Researcher): Boolean {
        trade.mapInfo?.let { mapInfo ->
            val structId = getStructTagOrKey(mapInfo.structure)
            if (player is ServerPlayer && StructureAdvancements.playerHasFoundStructure(player, structId)) {
                return false
            }
        }
        return true
    }

    fun getAllTrades(): List<ResearcherItemListing> {
        return if (loaded) {
            trades
        } else {
            RuinsOfGrowsseth.LOGGER.warn("Tried to load reasearcher trades before they were properly loaded, will return only fixed trades")
            TradesListener.FIXED_TRADES.map(ResearcherTradeEntry::itemListing)
        }
    }

    private fun getUnlockedTradesByStruct(): List<ResearcherTradeEntry> {
        val spawnsById = RemoteStructures.STRUCTS_TO_SPAWN_BY_ID
        val unlockableTrades = TradesListener.UNLOCKABLE_TRADES_BY_STRUCT
        return unlockableTrades.filter { (id, _) -> spawnsById.any { (spawnId, structureSpawn) ->
            structureSpawn.structure.path == id && fixedStructureGeneration.spawnedQueuedStructure(spawnId) == false
        } }.flatMap { it.value }
    }

    private fun getUnlockedTradesByEvent(events: List<ApiEvent>): List<ResearcherTradeEntry> {
        val unlockableTrades = TradesListener.UNLOCKABLE_TRADES_BY_EVENT
        return unlockableTrades.filter { (name, trades) -> events.any { event ->
            event.active && event.name == name
        } }.flatMap { it.value }
    }

    // Translate events named like event:sell/growsseth/growsseth_banner_pattern/5 (sell/<namespace>/<item id>/<emerald cost>)
    // Deprecated (JSON-side): use custom trades instead [getCustomTrades]
    private fun getSimpleEventTrades(events: List<ApiEvent>): List<ResearcherTradeEntry> {
        val eventTradeInfos = events.mapNotNull { tradeEvent ->
            if (tradeEvent.name.contains("sell/") && tradeEvent.active) {
                if (tradeEvent.name.matches(Regex("sell/\\w+/\\w+/\\d+(/\\w+)?"))) {
                    val split = tradeEvent.name.replace("sell/", "").split("/")
                    val item = itemFromId(ResourceLocation(split[0], split[1]))
                    val flags = split.getOrNull(3)
                    Triple(item, split[2].toInt(), flags)
                } else {
                    RuinsOfGrowsseth.LOGGER.error("Trade event ${tradeEvent.name} wrongly formatted")
                    null
                }
            } else {
                null
            }
        }

        return eventTradeInfos.map { (item, price, flags) ->
            val hideNotif = flags?.contains('h') == true
            ResearcherTradeEntry(
                ResearcherItemListing(
                    ItemStack(item, 1), listOf(ItemStack(Items.EMERALD, price)), 99,
                    noNotification = hideNotif,
                ),
                100,
            )
        }
    }

    private fun getCustomTrades(events: List<ApiEvent>): List<ResearcherTradeEntry> {
        val eventTradeInfos = events.filter { event -> event.name.startsWith("customTrade/") && event.active }
        return eventTradeInfos.mapNotNull { tradeEvent ->
            val desc = tradeEvent.desc ?: run {
                RuinsOfGrowsseth.LOGGER.error("Custom trade event $tradeEvent doesn't have a desc")
                return@mapNotNull null
            }
            try {
                JSON.decodeFromString(ResearcherTradeObj.serializer(), desc).decode()
            } catch (e: Exception) {
                e.printStackTrace()
                RuinsOfGrowsseth.LOGGER.error("Custom trade event $tradeEvent has wrongly formatted JSON trade data")
                null
            }
        }
    }

    fun getUnlockedTradesByQuest(quests: List<ApiQuestData>): List<ResearcherTradeEntry> {
        val unlockableTrades = TradesListener.UNLOCKABLE_TRADES_BY_QUEST
        return unlockableTrades.filter { (id, trades) -> quests.any { quest ->
            quest.unlocked && quest.id == id
        } }.flatMap { it.value }
    }

    // Should be ran once per item stack
    fun setTradeMapTarget(entity: Entity, itemStack: ItemStack, mapData: TradeItemMapInfo, offer: MerchantOffer) {
        // entity is Entity due to ItemListing signature, but de facto it should always be a ResearcherEntity
        if (entity !is Researcher) {
            RuinsOfGrowsseth.LOGGER.error("Tried setting trade map target $mapData to non researcher entity $entity!")
            return
        }
        // After this, Kotlin auto casts it to Researcher

        val scale = mapData.scale ?: 3
        var known = false

        synchronized(entity.storedMapLocations) {
            entity.storedMapLocations[mapData.structure]?.let { mapMemory ->
                val destinationType = mapMemory.struct.map({
                    DestinationType.auto(it, entity.server!!.registryAccess())
                }, {
                    DestinationType.auto(it)
                })
                updateMapToPos(
                    itemStack,
                    entity.level() as ServerLevel,
                    mapMemory.pos,
                    scale,
                    destinationType = destinationType,
                    displayName = mapData.name,
                )
                val mapSavedData = MapItem.getSavedData(mapMemory.mapId, entity.level())
                if (mapSavedData != null) {
                    MapItemAccessor.callStoreMapData(itemStack, mapMemory.mapId)
                } else {
                    RuinsOfGrowsseth.LOGGER.info(
                        "Tried setting res. known map data from id ${mapMemory.mapId} but was null, making new"
                    )
                }
                RuinsOfGrowsseth.LOGGER.info("Loaded map data from known map $mapMemory")
                known = true
            }
        }

        if (!known) {
            var pos: BlockPos? = null
            if (mapData.x != null && mapData.z != null) {
                pos = BlockPos(mapData.x, getYAtXZ(entity.level() as ServerLevel, mapData.x, mapData.z), mapData.z)
            } else if (mapData.fixedStructureId != null) {
                val id = ResourceLocation(mapData.fixedStructureId)
                val spawnData = RemoteStructures.STRUCTS_TO_SPAWN_BY_ID.values
                    .filter { it.structure == id }
                    .minByOrNull { it.pos.distManhattan(entity.blockPosition()) }
                if (spawnData != null) {
                    pos = spawnData.pos
                } else {
                    RuinsOfGrowsseth.LOGGER.warn("Map $mapData has structure id not found in fixed spawns: ${mapData.fixedStructureId}")
                }
            }
            if (pos != null) {
                val dest = getStructTagOrKey(mapData.structure)
                val destinationType = dest.map({
                    DestinationType.auto(it, entity.server!!.registryAccess())
                }, {
                    DestinationType.auto(it)
                })

                updateMapToPos(
                    itemStack,
                    entity.level() as ServerLevel,
                    pos,
                    scale,
                    destinationType = destinationType,
                    displayName = mapData.name,
                )
                RuinsOfGrowsseth.LOGGER.info("Res.trades: created map to pos $pos dtype $destinationType")

                synchronized(entity.storedMapLocations) {
                    entity.storedMapLocations[mapData.structure] = Researcher.MapMemory(
                        pos,
                        dest,
                        MapItem.getMapId(itemStack) ?: throw IllegalStateException("Map has no id after updating pos! $itemStack"),
                    )
                }
                entity.refreshCurrentTrades()
                known = true
            }
        }

        if (!known) {
            // For community version (locate map functionality)
            updateMapToStruct(
                itemStack,
                entity.level() as ServerLevel,
                mapData.structure,
                entity.blockPosition(),
                scale,
                displayName = mapData.name,
                async = true,
            ).thenAccept {
                val pos = it.first
                if (pos != null) {
                    RuinsOfGrowsseth.LOGGER.info("Res.trades: found map to pos $pos struct ${mapData.structure}")
                    synchronized(entity.storedMapLocations) {
                        entity.storedMapLocations[mapData.structure] = Researcher.MapMemory(
                            pos,
                            Either.right(it.second.unwrapKey().get()),
                            MapItem.getMapId(itemStack) ?: throw IllegalStateException("Map item has no id after updating! $itemStack"),
                        )
                    }
                    entity.refreshCurrentTrades()
                } else {
                    offer.increaseUses() // Disable offer, as it's always generated with 1 use
                }
            }
        }
    }

    // Save data for all researcher trades directly in world save, to make notifying for new ones easier
    private class TradesSavedData : SavedData() {
        companion object {
            private val factory: Factory<TradesSavedData> = Factory({ TradesSavedData() }, Companion::load, DataFixTypes.ENTITY_CHUNK )

            fun get(server: MinecraftServer): MutableList<ResearcherItemListing> {
                val level = server.overworld()
                return level.dataStorage.computeIfAbsent(factory, "researcher_trades").trades
            }

            fun setDirty(server: MinecraftServer) {
                val level = server.overworld()
                level.dataStorage.get<TradesSavedData>(factory, "researcher_trades")?.setDirty()
            }

            fun load(compoundTag: CompoundTag): TradesSavedData {
                val out = TradesSavedData()
                out.trades.addAll(compoundTag.loadField("allTrades", ResearcherItemListing.MLIST_CODEC) ?: listOf())
                return out
            }
        }

        val trades = mutableListOf<ResearcherItemListing>()

        override fun save(compoundTag: CompoundTag): CompoundTag {
            return compoundTag.also {
                it.saveField("allTrades", ResearcherItemListing.MLIST_CODEC, this::trades)
            }
        }
    }
}

