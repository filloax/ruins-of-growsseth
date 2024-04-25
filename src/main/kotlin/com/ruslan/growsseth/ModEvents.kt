package com.ruslan.growsseth

import com.filloax.fxlib.platform.ServerEvent
import com.ruslan.growsseth.advancements.GrowssethAdvancements
import com.ruslan.growsseth.advancements.StructureAdvancements
import com.ruslan.growsseth.advancements.criterion.JigsawPieceTrigger
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.dialogues.BasicDialoguesComponent
import com.ruslan.growsseth.entity.researcher.CustomRemoteDiaries
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ResearcherDialoguesComponent
import com.ruslan.growsseth.entity.researcher.ResearcherDiaryComponent
import com.ruslan.growsseth.entity.researcher.trades.GlobalResearcherTradesProvider
import com.ruslan.growsseth.entity.researcher.trades.ProgressResearcherTradesProvider
import com.ruslan.growsseth.http.DataRemoteSync
import com.ruslan.growsseth.http.GrowssethApiV1
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.http.LiveUpdatesConnection
import com.ruslan.growsseth.loot.VanillaStructureLoot
import com.ruslan.growsseth.quests.QuestComponentEvents
import com.ruslan.growsseth.structure.RemoteStructureBooks
import com.ruslan.growsseth.structure.StructureDisabler
import com.ruslan.growsseth.structure.VillageBuildings
import com.ruslan.growsseth.utils.AsyncLocator
import com.ruslan.growsseth.utils.MixinHelpers
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import com.ruslan.growsseth.worldgen.worldpreset.LocationNotifListener
import net.fabricmc.fabric.api.loot.v2.LootTableSource
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.SectionPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager
import net.minecraft.world.level.storage.loot.LootDataManager
import net.minecraft.world.level.storage.loot.LootTable

abstract class ModEvents {
    companion object {
        fun get() = FabricEvents
    }

    fun initCallbacks() {
        onServerStarting { server ->
            AsyncLocator.handleServerAboutToStartEvent()
            DataRemoteSync.handleServerAboutToStartEvent(server)
            DataRemoteSync.doSync(WebConfig.dataSyncUrl, server)
            MixinHelpers.serverInit(server)
            LiveUpdatesConnection.serverStart(server)
        }
        onServerStarted { server ->
            GrowssethWorldPreset.Callbacks.onServerStarted(server)
            VillageBuildings.onServerStarted(server)
            ProgressResearcherTradesProvider.Callbacks.onServerStarted(server)
        }
        onServerStopping { server ->
            AsyncLocator.handleServerStoppingEvent()
            DataRemoteSync.handleServerStoppingEvent()
            GrowssethApiV1.Callbacks.onServerStop(server)
            GlobalResearcherTradesProvider.Callbacks.onServerStop(server)
            LiveUpdatesConnection.serverStop(server)
            GrowssethExtraEvents.onServerStop()
            CustomRemoteDiaries.onServerStopped()
            RemoteStructureBooks.onServerStopped()
        }
        onServerStopped { server ->
            LocationNotifListener.Callbacks.onServerStopped(server)
        }
        onServerLevelLoad { server, level ->
            DataRemoteSync.handleWorldLoaded(server, level)
            ResearcherDiaryComponent.Callbacks.onServerLevel(level)
        }
        onStartServerTick { server ->
            DataRemoteSync.checkTickSync(WebConfig.dataSyncUrl, server)
            GrowssethAdvancements.Callbacks.onServerTick(server)
        }

        onEndServerLevelTick { level ->
            level.players().forEach(::onServerPlayerTick)
        }

        onLoadChunk { level, chunk ->
            GrowssethExtraEvents.Callbacks.onLoadChunk(level, chunk)
        }

        onEntityLoad { entity, level ->
            QuestComponentEvents.onLoadEntity(entity)
        }
        onEntityUnload { entity, level ->
//            QuestComponentEvents.onUnloadEntity(entity, level)
        }
        onEntityDestroyed { entity, level ->
            Researcher.Callbacks.onEntityDestroyed(entity, level)
        }

        afterPlayerBlockBreak { level, player, pos, state, entity ->
            ResearcherDialoguesComponent.Callbacks.onBlockBreak(level, player, pos, state, entity)
        }
        afterPlayerPlaceBlock { player, world, pos, placeContext, blockState, item ->
            ResearcherDialoguesComponent.Callbacks.onPlaceBlock(player, world, pos, placeContext, blockState, item)
        }

        onPlayerServerJoin { handler, sender, server ->
            GlobalResearcherTradesProvider.Callbacks.onServerPlayerJoin(handler, sender, server)
            GrowssethExtraEvents.onServerPlayerJoin(handler, sender, server)
            GrowssethWorldPreset.Callbacks.onServerPlayerJoin(handler, sender, server)
        }

        onPlayerAdvancement { player, advancement, criterionString ->
            BasicDialoguesComponent.Callbacks.onAdvancement(player, advancement, criterionString)
            StructureAdvancements.Callbacks.onAdvancement(player, advancement, criterionString)
        }

        onFenceLeash { mob, pos, player ->
            Researcher.Callbacks.onFenceLeash(mob, pos, player)
        }
        onFenceUnleash { mob, pos ->
            Researcher.Callbacks.onFenceUnleash(mob, pos)
        }

        // Register singularly because returns
        beforeNameTagRename(Researcher.Callbacks::nameTagRename)

        beforeStructureGenerate { level, structure, _, _, _, _, _, _, _, _ ->
            StructureDisabler.Callbacks.shouldDisableStructure(structure, level)
        }

        onLootTableModify { resourceManager, lootManager, id, tableBuilder, source ->
            VanillaStructureLoot.onModifyLootTables(resourceManager, lootManager, id, tableBuilder, source)
        }
    }

    private fun onServerPlayerTick(player: ServerPlayer) {
        JigsawPieceTrigger.Callbacks.onServerPlayerTick(player)
    }

    fun triggerOnStructureFound(player: ServerPlayer, structId: ResourceKey<Structure>, isJigsawPart: Boolean) {
        ProgressResearcherTradesProvider.Callbacks.onStructureFound(player, structId, isJigsawPart)
    }
    
    abstract fun onServerStarting(event: ServerEvent)
    abstract fun onServerStarted(event: ServerEvent)
    abstract fun onServerStopping(event: ServerEvent)
    abstract fun onServerStopped(event: ServerEvent)
    abstract fun onServerLevelLoad(event: (MinecraftServer, ServerLevel) -> Unit)
    abstract fun onStartServerTick(event: ServerEvent)
    abstract fun onEndServerLevelTick(event: (ServerLevel) -> Unit)
    abstract fun onLoadChunk(event: (level: ServerLevel, chunk: LevelChunk) -> Unit)
    abstract fun onEntityLoad(event: (entity: Entity, level: ServerLevel) -> Unit)
    abstract fun onEntityUnload(event: (entity: Entity, level: ServerLevel) -> Unit)
    abstract fun onEntityDestroyed(event: (entity: Entity, level: ServerLevel) -> Unit)
    abstract fun afterPlayerBlockBreak(event: (Level, Player, BlockPos, BlockState, BlockEntity?) -> Unit)
    abstract fun afterPlayerPlaceBlock(event: (Player, Level, BlockPos, BlockPlaceContext, BlockState, BlockItem) -> Unit)
    abstract fun onPlayerServerJoin(event: (handler: ServerGamePacketListenerImpl, PacketSender, MinecraftServer) -> Unit)
    abstract fun onPlayerAdvancement(event: (ServerPlayer, AdvancementHolder, criterionString: String) -> Unit)
    abstract fun onFenceLeash(event: (Mob, BlockPos, ServerPlayer) -> Unit)
    abstract fun onFenceUnleash(event: (Mob, BlockPos) -> Unit)
    abstract fun beforeNameTagRename(event: (target: LivingEntity, Component, ServerPlayer, ItemStack, InteractionHand) -> InteractionResultHolder<ItemStack>)
    abstract fun onLootTableModify(event: (resourceManager: ResourceManager, lootManager: LootDataManager, id: ResourceLocation, tableBuilder: LootTable.Builder, source: LootTableSource) -> Unit)

    /**
     * Returns true if structure should not spawn
     */
    abstract fun beforeStructureGenerate(event: (
        ServerLevel, structure: Holder<Structure>, StructureManager, RegistryAccess,
        RandomState, StructureTemplateManager, seed: Long,
        ChunkAccess, ChunkPos, SectionPos
    ) -> Boolean)
}

interface Modify {
    /**
     * Called when a loot table is loading to modify loot tables.
     *
     * @param resourceManager the server resource manager
     * @param lootManager     the loot manager
     * @param id              the loot table ID
     * @param tableBuilder    a builder of the loot table being loaded
     * @param source          the source of the loot table
     */
    fun modifyLootTable(
        resourceManager: ResourceManager?,
        lootManager: LootDataManager?,
        id: ResourceLocation?,
        tableBuilder: LootTable.Builder?,
        source: LootTableSource?
    )
}