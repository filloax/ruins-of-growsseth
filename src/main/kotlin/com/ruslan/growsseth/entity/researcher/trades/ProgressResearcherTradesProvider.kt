package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.codec.constructorWithOptionals
import com.filloax.fxlib.codec.forNullableGetter
import com.filloax.fxlib.codec.mutableSetCodec
import com.filloax.fxlib.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.levelgen.structure.Structure
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

    init {
        this.init()
        numInstances++
        if (numInstances > 2) {
            throw IllegalStateException("Instanced more than two ProgressResearcherTradesProvider: $this")
        }
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
        val time = researcher.level().gameTime
        var randomTrades = data.randomTrades
        if (randomTrades == null || data.lastRandomTradeChangeTime < 0 || time - data.lastRandomTradeChangeTime > Constants.DAY_TICKS_DURATION) {
            data.lastRandomTradeChangeTime = time
            randomTrades = genRandomTrades(player, researcher, data)
        }

        data.randomTrades = randomTrades
        return randomTrades
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
        val tradesBefore = TradesListener.TRADES_PROGRESS_BEFORE_STRUCTURE.filterKeys {
            val key = ResourceKey.create(Registries.STRUCTURE, resLoc(it))
            data.currentStructure == key || data.foundStructures.contains(key)
        }.values.flatten()
        val tradesAfter = TradesListener.TRADES_PROGRESS_AFTER_STRUCTURE.filterKeys {
            val key = ResourceKey.create(Registries.STRUCTURE, resLoc(it))
            data.foundStructures.contains(key)
        }.values.flatten()
        val allTrades = tradesBefore + tradesAfter
        val changedMapPriority = allTrades.map {
            val toStruct = it.itemListing.mapInfo?.structure ?: it.itemListing.mapInfo?.fixedStructureId
            if (toStruct != null && ResourceLocation(toStruct) != data.currentStructure?.location()) {
                // Increase priority for maps of not current structure to reduce clogging of UI
                it.copy(priority = it.priority + 1000)
            } else {
                it
            }
        }
        return changedMapPriority
    }

    private fun genRandomTrades(player: ServerPlayer, researcher: Researcher, tradesData: ResearcherTradesData): List<ResearcherTradeEntry> {
        val data = ProgressTradesSavedData.get(player.server)
        val daysOffset = (researcher.level().gameTime % Constants.DAY_TICKS_DURATION) * 1323
        val random = Random(player.server.overworld().seed + (researcher.persistId ?: 1) * 10 + daysOffset)
        val possibleTrades = TradesListener.TRADES_PROGRESS_AFTER_STRUCTURE_RANDOM.filterKeys {
            val key = ResourceKey.create(Registries.STRUCTURE, resLoc(it))
            data.currentStructure == key || data.foundStructures.contains(key)
        }.values.flatten()
        val amount = IntRange(
            min(ResearcherConfig.randomTradeNumItems.min, possibleTrades.size),
            min(ResearcherConfig.randomTradeNumItems.max, possibleTrades.size),
        ).random(random)

        if (amount > 0)
            return possibleTrades.shuffled(random).subList(0, amount)
        else
            return listOf()
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
            if (!prov.isEnabled(server)) return

            val foundStructures = ProgressTradesSavedData.get(server).foundStructures
            if (foundStructures.contains(structId)) return

            foundStructures.add(structId)
            ProgressTradesSavedData.setDirty(server)
            prov.regenTrades(server)
        }
    }
}