package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.itemFromId
import com.filloax.fxlib.json.KotlinJsonResourceReloadListener
import com.filloax.fxlib.loreLines
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

// Instanced in init class RuinsOfGrowsseth
class TradesListener : KotlinJsonResourceReloadListener(JSON, Constants.TRADES_DATA_FOLDER) {
    companion object {
        private val JSON = Json { isLenient = true }
        var ready = false
            private set

        val FIXED_TRADES = mutableListOf<ResearcherTradeEntry>()
        val UNLOCKABLE_TRADES_BY_STRUCT = mutableMapOf<String, MutableList<ResearcherTradeEntry>>()
        val UNLOCKABLE_TRADES_BY_QUEST = mutableMapOf<Int, MutableList<ResearcherTradeEntry>>()
        val UNLOCKABLE_TRADES_BY_EVENT = mutableMapOf<String, MutableList<ResearcherTradeEntry>>()
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        FIXED_TRADES.clear()
        UNLOCKABLE_TRADES_BY_STRUCT.clear()
        UNLOCKABLE_TRADES_BY_QUEST.clear()
        UNLOCKABLE_TRADES_BY_EVENT.clear()
        loader.forEach { (fileIdentifier, jsonElement) ->
            try {
                RuinsOfGrowsseth.LOGGER.debug("Read json trades file {}", fileIdentifier)
                val entries = JSON.decodeFromJsonElement(TradesObj.serializer(), jsonElement)
                entries.fixedTrades?.let { trades -> FIXED_TRADES.addAll(trades.map{ it.decode() }) }
                entries.unlockableByStructure?.let { trades -> UNLOCKABLE_TRADES_BY_STRUCT.putAll(trades.mapValues { e -> e.value.map { it.decode() }.toMutableList() }) }
                entries.unlockableByEvent?.let { trades -> UNLOCKABLE_TRADES_BY_EVENT.putAll(trades.mapValues { e -> e.value.map { it.decode() }.toMutableList() }) }
                entries.unlockableByQuest?.let { trades -> UNLOCKABLE_TRADES_BY_QUEST.putAll(
                    trades.mapValues { e -> e.value.map { it.decode() }.toMutableList() }
                        .mapKeys { k -> k.key.toInt() }
                )}

            } catch (e: Exception) {
                RuinsOfGrowsseth.LOGGER.error( "Growsseth: Couldn't parse trades file {}", fileIdentifier, e)
            }
        }
        ready = true
    }

    @Serializable
    data class TradesObj(
        val fixedTrades: List<ResearcherTradeObj>? = null,
        val unlockableByStructure: Map<String, List<ResearcherTradeObj>>? = null,
        val unlockableByQuest: Map<String, List<ResearcherTradeObj>>? = null,
        val unlockableByEvent: Map<String, List<ResearcherTradeObj>>? = null,
    )
}

/**
 * Refer to [ResearcherItemListing] for explanation of parameters or similar parameters.
 */
@Serializable
data class ResearcherTradeObj(
    val gives: TradeItemEntryObj,
    val wants: List<TradeItemEntryObj>,
    val priority: Int = 0,
    val noNotification: Boolean = false,
    val replace: Boolean = false,
) {
    init {
        assert(wants.size in 1..2) { "Size of wants wrong (must be 1 or 2)" }
        assert(wants.all { itemFromId(it.id) != Items.AIR }) { "Wants items invalid: $wants" }
        assert(itemFromId(gives.id) != Items.AIR) { "Gives item invalid: $gives" }
    }
    fun decode(): ResearcherTradeEntry {
        return ResearcherTradeEntry(
            itemListing = ResearcherItemListing(
                gives.toItemStack(),
                wants.map { it.toItemStack() },
                99,
                gives.map?.unwrap(),
                gives.diaryId,
                noNotification = noNotification,
            ),
            priority = priority,
            replace = replace,
        )
    }

    @Serializable
    data class TradeItemEntryObj (
        val id: String,
        val amount: Int = 1,
        val map: TradeItemMapInfo.JsonDesc? = null,
        val diaryId: String? = null,
    ) {
        fun toItemStack(): ItemStack {
            val itemStack = ItemStack(itemFromId(id), amount)
            // Add description here so it only gets added once
            map?.unwrap()?.description?.forEach { itemStack.loreLines().add(Component.translatable(it)) }
            return itemStack
        }
    }
}