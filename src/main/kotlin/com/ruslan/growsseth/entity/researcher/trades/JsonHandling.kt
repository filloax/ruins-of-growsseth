package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.itemFromId
import com.filloax.fxlib.json.KotlinJsonResourceReloadListener
import com.filloax.fxlib.loreLines
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

// Instanced in init class RuinsOfGrowsseth
class TradesListener : KotlinJsonResourceReloadListener(JSON, Constants.TRADES_DATA_FOLDER) {
    companion object {
        private val JSON = Json { isLenient = true }
        var ready = false
            private set

        private val INFINITE_MAX_USES = 99999

        val FIXED_TRADES_WHEN_RANDOM = mutableListOf<ResearcherTradeEntry>()
        val RANDOM_TRADES_POOL = mutableListOf<ResearcherTradeEntry>()
        val WEB_UNLOCKABLE_TRADES_BY_STRUCT = mutableMapOf<String, MutableList<ResearcherTradeEntry>>()
        val WEB_UNLOCKABLE_TRADES_BY_EVENT = mutableMapOf<String, MutableList<ResearcherTradeEntry>>()
        val TRADES_BEFORE_STRUCTURE = mutableMapOf<String, MutableList<ResearcherTradeEntry>>()
        val TRADES_PROGRESS_AFTER_STRUCTURE = mutableMapOf<String, MutableList<ResearcherTradeEntry>>()
        val TRADES_PROGRESS_AFTER_STRUCTURE_RANDOM = mutableMapOf<String, MutableList<ResearcherTradeEntry>>()
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        FIXED_TRADES_WHEN_RANDOM.clear()
        WEB_UNLOCKABLE_TRADES_BY_STRUCT.clear()
        WEB_UNLOCKABLE_TRADES_BY_EVENT.clear()
        RANDOM_TRADES_POOL.clear()
        TRADES_BEFORE_STRUCTURE.clear()
        TRADES_PROGRESS_AFTER_STRUCTURE.clear()
        TRADES_PROGRESS_AFTER_STRUCTURE_RANDOM.clear()
        loader.forEach { (fileIdentifier, jsonElement) ->
            RuinsOfGrowsseth.LOGGER.debug("Read json trades file {}", fileIdentifier)
            val entries = JSON.decodeFromJsonElement(TradesObj.serializer(), jsonElement)
            addAllTo(entries.fixedTradesWhenRandom, FIXED_TRADES_WHEN_RANDOM)
            addAllTo(entries.unlockableByRemoteStructure?.modifyForWebTrades(), WEB_UNLOCKABLE_TRADES_BY_STRUCT)
            addAllTo(entries.unlockableByRemoteEvent?.modifyForWebTrades(), WEB_UNLOCKABLE_TRADES_BY_EVENT)
            addAllTo(entries.randomPool, RANDOM_TRADES_POOL)
            addAllTo(entries.beforeStructure, TRADES_BEFORE_STRUCTURE)
            addAllTo(entries.progressAfterStructure, TRADES_PROGRESS_AFTER_STRUCTURE)
            addAllTo(entries.progressAfterStructureRandom, TRADES_PROGRESS_AFTER_STRUCTURE_RANDOM)
        }
        ready = true
    }

    private fun addAllTo(from: Map<String, List<ResearcherTradeObj>>?, to: MutableMap<String, MutableList<ResearcherTradeEntry>>) {
        from?.let { trades -> to.mergeAll(trades.mapValues { e -> e.value.map { it.decode() } }) }
    }
    private fun addAllTo(from: List<ResearcherTradeObj>?, to: MutableList<ResearcherTradeEntry>) {
        from?.let { trades -> to.addAll(trades.map{ it.decode() }) }
    }
    private fun <K, V> MutableMap<K, MutableList<V>>.mergeAll(other: Map<K, List<V>>) {
        forEach { (key, list) ->
            other[key]?.let { list.addAll(it) }
        }
        other.forEach { (key, list) ->
            if (!containsKey(key))
                this[key] = list.toMutableList()
        }
    }
    private fun Map<String, List<ResearcherTradeObj>>.modifyForWebTrades() = mapValues { e -> e.value.map { it.copy(maxUses = INFINITE_MAX_USES) } }

    @Serializable
    data class TradesObj(
        val fixedTradesWhenRandom: List<ResearcherTradeObj>? = null,
        val unlockableByRemoteStructure: Map<String, List<ResearcherTradeObj>>? = null,
        val unlockableByRemoteEvent: Map<String, List<ResearcherTradeObj>>? = null,
        val beforeStructure: Map<String, List<ResearcherTradeObj>>? = null,
        val progressAfterStructure: Map<String, List<ResearcherTradeObj>>? = null,
        val progressAfterStructureRandom: Map<String, List<ResearcherTradeObj>>? = null,
        val randomPool: List<ResearcherTradeObj>? = null,
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
    val randomWeight: Float = 1f,
    val maxUses: Int = 1,
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
                maxUses,
                gives.map?.unwrap(),
                gives.diaryId,
                noNotification = noNotification,
                randomWeight = randomWeight,
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

    companion object {
        fun tradeIdemEntryObj(id: ResourceLocation, amount: Int = 1, map: TradeItemMapInfo.JsonDesc? = null, diaryId: String? = null) = TradeItemEntryObj(
            id.toString(), amount, map, diaryId
        )
        fun tradeIdemEntryObj(item: Item, amount: Int = 1, map: TradeItemMapInfo.JsonDesc? = null, diaryId: String? = null) = tradeIdemEntryObj(
            BuiltInRegistries.ITEM.getKey(item), amount, map, diaryId
        )
    }
}

