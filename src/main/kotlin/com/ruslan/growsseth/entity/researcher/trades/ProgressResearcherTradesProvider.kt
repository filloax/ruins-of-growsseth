package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.codec.constructorWithOptionals
import com.filloax.fxlib.codec.forNullableGetter
import com.filloax.fxlib.codec.mutableSetCodec
import com.filloax.fxlib.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.advancements.StructureAdvancements
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ResearcherQuestComponent
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.level.levelgen.structure.Structure
import java.lang.IllegalArgumentException
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlin.random.Random

/**
 * If no preset order, unlock structures in random order (avoiding already discovered ones).
 * Similar to other researcher trade providers, meant for 1 instance per mode,
 * just this hanldes two similar mode at once so 2 instances (even if they should be mutually exclusive, with
 * one being tied to world preset).
 */
class ProgressResearcherTradesProvider(
    private val structures: List<ResourceKey<Structure>>,
    private val inOrder: Boolean = false,
) : GlobalResearcherTradesProvider() {
    override val mode: ResearcherTradeMode = if (inOrder)  ResearcherTradeMode.GROWSSETH_PROGRESS else ResearcherTradeMode.PROGRESS

    private val structByTags = structures.associateBy { GrowssethStructures.info[it]!!.tag }

    init {
        this.init()
        numInstances++
        if (numInstances > 2) {
            throw IllegalStateException("Instanced more than two ProgressResearcherTradesProvider: $this")
        }
    }

    fun onlyOneLeft(server: MinecraftServer) = ProgressTradesSavedData.get(server).let {
        it.currentStructure != null && it.foundStructures.size == structures.size - 1
    }

    fun isFinished(server: MinecraftServer) = ProgressTradesSavedData.get(server).let {
        it.currentStructure == null && it.foundStructures.isNotEmpty()
    }

    private fun regenTrades(server: MinecraftServer) {
        val data = ProgressTradesSavedData.get(server)

        checkAndSetCurrentStructure(server, data)

        val fixedTrades = genFixedTrades(server, data)
        applyUpdatedTrades(server, fixedTrades)
    }

    override fun getExtraPlayerTrades(player: ServerPlayer, researcher: Researcher, data: ResearcherTradesData): List<ResearcherTradeEntry> {
        val finishedQuest = researcher.quest!!.passedStage(ResearcherQuestComponent.Stages.HOME)
        val possibleTrades = getPossibleRandomTrades(player, researcher)
        val possibleTradesItems = possibleTrades.map { it.itemListing.gives.itemHolder }
        val tradesChanged = tradesDiffer(data.lastAvailableRandomTrades, possibleTradesItems)

        val time = researcher.level().gameTime
        var randomTrades = data.randomTrades
        if (
            randomTrades == null
            || tradesChanged
            || data.lastRandomTradeChangeTime < 0 || time - data.lastRandomTradeChangeTime > Constants.DAY_TICKS_DURATION
            || finishedQuest
        ) {
            data.lastRandomTradeChangeTime = time
            data.lastAvailableRandomTrades.clear()
            data.lastAvailableRandomTrades.addAll(possibleTradesItems)
            randomTrades = genRandomTrades(player, researcher, possibleTrades, finishedQuest)
        }

        data.randomTrades = randomTrades
        return randomTrades
    }

    private fun tradesDiffer(tradesOld: List<Holder<Item>>, tradesNew: List<Holder<Item>>): Boolean {
        if (tradesNew.size != tradesOld.size) return true

        for (i in tradesNew.indices) {
            if (tradesOld[i].value() != tradesNew[i].value())
                return true
        }

        return false
    }

    private fun checkAndSetCurrentStructure(server: MinecraftServer, data: ProgressTradesSavedData) {
        var currentStructure = data.currentStructure
        if (
            currentStructure != null && data.foundStructures.contains(currentStructure)
            || currentStructure == null && data.foundStructures.isEmpty()
        ) {
            currentStructure = getNewTargetStructure(server, data)
            data.currentStructure = currentStructure
            data.setDirty()
        }
    }

    private fun getNewTargetStructure(server: MinecraftServer, data: ProgressTradesSavedData): ResourceKey<Structure>? {
        if (inOrder) {
            for (structId in structures) {
                if (structId !in data.foundStructures) {
                    return structId
                }
            }
            return null
        } else {
            val possibleStructures = structures.minus(data.foundStructures)
            return possibleStructures.randomOrNull(Random(server.overworld().seed))
        }
    }

    private fun genFixedTrades(server: MinecraftServer, data: ProgressTradesSavedData): List<ResearcherTradeEntry> {
        val tradesBefore = TradesListener.TRADES_BEFORE_STRUCTURE.filterKeys {
            val key = ResourceKey.create(Registries.STRUCTURE, resLoc(it))
            data.currentStructure == key || data.foundStructures.contains(key)
        }.values.flatten()
        val tradesAfter = TradesListener.TRADES_PROGRESS_AFTER_STRUCTURE.filterKeys {
            val key = ResourceKey.create(Registries.STRUCTURE, resLoc(it))
            data.foundStructures.contains(key)
        }.values.flatten()
        val allTrades = tradesBefore + tradesAfter
        val changedMapPriority = allTrades.map { trade ->
            val toStruct = trade.itemListing.mapInfo?.structure ?: trade.itemListing.mapInfo?.fixedStructureId ?: return@map trade
            val matchingStructures = ResearcherTradeUtils.getMatchingStructures(server.registryAccess(), toStruct)
            if (!matchingStructures.contains(data.currentStructure?.location())) {
                // Increase priority for maps of not current structure to reduce clogging of UI
                trade.copy(priority = trade.priority + 1000)
            } else {
                trade
            }
        }
        return changedMapPriority
    }

    private fun getPossibleRandomTrades(player: ServerPlayer, researcher: Researcher): List<ResearcherTradeEntry> {
        return TradesListener.TRADES_PROGRESS_AFTER_STRUCTURE_RANDOM.filterKeys {
            val key = ResourceKey.create(Registries.STRUCTURE, resLoc(it))
            val tag = GrowssethStructures.info[key]!!.tag
            StructureAdvancements.playerHasFoundStructure(player, tag)
        }.values.flatten()
    }

    private fun genRandomTrades(player: ServerPlayer, researcher: Researcher, possibleTrades: List<ResearcherTradeEntry>, allTrades: Boolean = false): List<ResearcherTradeEntry> {
        val daysOffset = researcher.level().gameTime * 1323
        val random = Random(player.server.overworld().seed + researcher.uuid.hashCode() * 10 + daysOffset)
        return if (allTrades) {
            possibleTrades
        } else {
            val amount = IntRange(
                min(ResearcherConfig.randomTradeNumItems.min, possibleTrades.size),
                min(ResearcherConfig.randomTradeNumItems.max, possibleTrades.size),
            ).random(random)

            return if (amount > 0)
                possibleTrades.shuffled(random).subList(0, amount)
            else
                listOf()
        }
    }

    private fun getReferenceStructure(structId: ResourceKey<Structure>): ResourceKey<Structure>? {
        val tag = GrowssethStructures.info[structId]!!.tag
        return structByTags[tag]
    }

    companion object {
        private fun getCurrent(server: MinecraftServer): ProgressResearcherTradesProvider? {
            return ResearcherTradeMode.providerFromSettings(server).let { if (it is ProgressResearcherTradesProvider) it else null }
        }

        private var numInstances = 0
    }

    // Just one for both instances of this, as they are mutually exclusive with one depending on world type
    class ProgressTradesSavedData(
        val foundStructures: MutableSet<ResourceKey<Structure>> = mutableSetOf(),
        var currentStructure: ResourceKey<Structure>? = null,
    ) : FxSavedData<ProgressTradesSavedData>(CODEC) {
        companion object {
            val CODEC: Codec<ProgressTradesSavedData> = RecordCodecBuilder.create { builder -> builder.group(
                mutableSetCodec(ResourceKey.codec(Registries.STRUCTURE)).fieldOf("foundStructures").forGetter(ProgressTradesSavedData::foundStructures),
                ResourceKey.codec(Registries.STRUCTURE).optionalFieldOf("currentStructure").forNullableGetter(ProgressTradesSavedData::currentStructure),
            ).apply(builder, constructorWithOptionals(ProgressTradesSavedData::class)::newInstance) }
            private val DEF = define("progressTrades", ::ProgressTradesSavedData, CODEC)
            fun get(server: MinecraftServer) = server.loadData(DEF)
            fun setDirty(server: MinecraftServer) = server.loadData(DEF).setDirty()
        }

        // Check if struct matches
        fun alreadyFoundStructure(server: MinecraftServer, structId: ResourceKey<Structure>): Boolean {
            val tags = foundStructures.map { GrowssethStructures.info[it]!!.tag }
            val matchingStructures = tags.flatMap { tag ->
                server.registryAccess().registryOrThrow(Registries.STRUCTURE).getTagOrEmpty(tag).mapNotNull{it.unwrapKey().getOrNull()}
            }
            return matchingStructures.contains(structId)
        }
    }

    object Callbacks {
        fun onServerStarted(server: MinecraftServer) {
            val prov = getCurrent(server) ?: return
            if (!prov.isEnabled(server)) return
            prov.regenTrades(server)
        }

        fun onStructureFound(player: ServerPlayer, structId: ResourceKey<Structure>, isJigsawPart: Boolean) {
            if (structId.location().namespace != RuinsOfGrowsseth.MOD_ID) return

            val server = player.server
            val prov = getCurrent(server) ?: return

            if (prov.getReferenceStructure(structId) == null) return
            if (!prov.isEnabled(server)) return

            val data = ProgressTradesSavedData.get(server)
            if (data.alreadyFoundStructure(player.server, structId)) return

            data.foundStructures.add(prov.getReferenceStructure(structId)!!)
            ProgressTradesSavedData.setDirty(server)
            prov.regenTrades(server)
        }
    }
}