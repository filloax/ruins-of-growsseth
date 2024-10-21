package com.ruslan.growsseth

import com.filloax.fxlib.api.platform.ServiceUtil
import com.filloax.fxlib.platform.ServerEvent
import com.ruslan.growsseth.advancements.GrowssethAdvancements
import com.ruslan.growsseth.advancements.StructureAdvancements
import com.ruslan.growsseth.advancements.criterion.JigsawPieceTrigger
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.dialogues.BasicDialoguesComponent
import com.ruslan.growsseth.entity.researcher.*
import com.ruslan.growsseth.entity.researcher.trades.GlobalResearcherTradesProvider
import com.ruslan.growsseth.entity.researcher.trades.ProgressResearcherTradesProvider
import com.ruslan.growsseth.events.Events
import com.ruslan.growsseth.http.*
import com.ruslan.growsseth.loot.LootTableModifier
import com.ruslan.growsseth.loot.VanillaStructureLoot
import com.ruslan.growsseth.quests.QuestComponentEvents
import com.ruslan.growsseth.structure.RemoteStructureBooks
import com.ruslan.growsseth.structure.VillageBuildings
import com.ruslan.growsseth.structure.locate.StoppableAsyncLocator
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import com.ruslan.growsseth.worldgen.worldpreset.LocationNotifListener
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.storage.loot.LootTable


abstract class ModEvents {
    companion object {
        fun get(): ModEvents = ServiceUtil.findService(ModEvents::class.java)
    }

    fun initCallbacks() {
        onServerStarting { server ->
            StoppableAsyncLocator.Callbacks.onServerStarting()
            DataRemoteSync.Callbacks.handleServerAboutToStartEvent(server)
            DataRemoteSync.doSync(WebConfig.dataSyncUrl, server)
            com.ruslan.growsseth.utils.MixinHelpers.serverInit(server)
            LiveUpdatesConnection.serverStart(server)
        }
        onServerStarted { server ->
            GrowssethWorldPreset.Callbacks.onServerStarted(server)
            VillageBuildings.onServerStarted(server)
            ProgressResearcherTradesProvider.Callbacks.onServerStarted(server)
        }
        onServerStopping { server ->
            StoppableAsyncLocator.Callbacks.onServerStopping()
            DataRemoteSync.Callbacks.handleServerStoppingEvent()
            GrowssethApiV2.Callbacks.onServerStop(server)
            GlobalResearcherTradesProvider.Callbacks.onServerStop(server)
            LiveUpdatesConnection.serverStop(server)
            GrowssethExtraEvents.onServerStop()
            CustomRemoteDiaries.onServerStopped()
            RemoteStructureBooks.onServerStopped()
        }
        onServerStopped { server ->
            LocationNotifListener.Callbacks.onServerStopped(server)
            ResearcherSavedData.Callbacks.onServerStopped(server)
        }
        onServerLevelLoad { server, level ->
            DataRemoteSync.Callbacks.onServerLevel(server, level)
            ResearcherDiaryComponent.Callbacks.onServerLevel(level)
        }
        onStartServerTick { server ->
            DataRemoteSync.Callbacks.onServerTick(WebConfig.dataSyncUrl, server)
            GrowssethAdvancements.Callbacks.onServerTick(server)
            ProgressResearcherTradesProvider.Callbacks.onServerTick(server)
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

        afterPlayerBlockBreak { level, player, pos, state ->
            ResearcherDialoguesComponent.Callbacks.onBlockBreak(level, player, pos, state)
        }

        onPlayerServerJoin { player, server ->
            GlobalResearcherTradesProvider.Callbacks.onServerPlayerJoin(player, server)
            GrowssethExtraEvents.onServerPlayerJoin(player, server)
            GrowssethWorldPreset.Callbacks.onServerPlayerJoin(player, server)
        }

        // Register singularly because returns

        onLootTableModify { key, table ->
            VanillaStructureLoot.onModifyLootTables(key, table)
        }

        registerCustomEvents()
    }

    private fun registerCustomEvents() {
        Events.PLACE_BLOCK += { ev ->
            ResearcherDialoguesComponent.Callbacks.onPlaceBlock(ev.player, ev.world, ev.pos, ev.placeContext, ev.blockState, ev.item)
        }
        Events.PLAYER_ADVANCEMENT += { ev ->
            val player = ev.player; val advancement = ev.advancement; val criterionString = ev.criterionKey;
            BasicDialoguesComponent.Callbacks.onAdvancement(player, advancement, criterionString)
            StructureAdvancements.Callbacks.onAdvancement(player, advancement, criterionString)
        }
        Events.FENCE_LEASH += { ev ->
            val mob = ev.mob; val pos = ev.pos; val player = ev.player;
            ResearcherDonkey.onFenceLeash(mob, pos, player)
        }
        Events.FENCE_UNLEASH += { ev ->
            val mob = ev.mob; val pos = ev.pos;
            ResearcherDonkey.onFenceUnleash(mob, pos)
        }
        Events.NAMETAG_PRE += { Researcher.Callbacks.nameTagRename(it.target, it.name, it.player, it.stack, it.usedHand) }
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
    abstract fun afterPlayerBlockBreak(event: (Level, Player, BlockPos, BlockState) -> Unit)
    abstract fun onPlayerServerJoin(event: (player: ServerPlayer, MinecraftServer) -> Unit)
    abstract fun onLootTableModify(event: (key: ResourceLocation, table: LootTableModifier) -> Unit)
}