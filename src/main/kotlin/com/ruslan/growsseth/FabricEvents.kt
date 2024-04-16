package com.ruslan.growsseth

import com.ruslan.growsseth.advancements.GrowssethAdvancements
import com.ruslan.growsseth.advancements.criterion.JigsawPieceTrigger
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.dialogues.BasicDialoguesComponent
import com.ruslan.growsseth.entity.researcher.CustomRemoteDiaries
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ResearcherDialoguesComponent
import com.ruslan.growsseth.entity.researcher.ResearcherDiaryComponent
import com.ruslan.growsseth.entity.researcher.trades.ResearcherTrades
import com.ruslan.growsseth.events.*
import com.ruslan.growsseth.http.DataRemoteSync
import com.ruslan.growsseth.http.GrowssethApiV1
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.http.LiveUpdatesConnection
import com.ruslan.growsseth.quests.QuestComponentEvents
import com.ruslan.growsseth.structure.RemoteStructureBooks
import com.ruslan.growsseth.structure.StructureDisabler
import com.ruslan.growsseth.structure.VillageBuildings
import com.ruslan.growsseth.utils.AsyncLocator
import com.ruslan.growsseth.utils.MixinHelpers
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import com.ruslan.growsseth.worldgen.worldpreset.LocationNotifListener
import net.fabricmc.fabric.api.event.lifecycle.v1.*
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerPlayer

object FabricEvents {
    fun initEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            AsyncLocator.handleServerAboutToStartEvent()
            DataRemoteSync.handleServerAboutToStartEvent(server)
            Researcher.initServer(server)
            DataRemoteSync.doSync(WebConfig.dataSyncUrl, server)
            MixinHelpers.serverInit(server)
            LiveUpdatesConnection.serverStart(server)
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            GrowssethWorldPreset.Callbacks.onServerStarted(server)
            VillageBuildings.onServerStarted(server)
        }
        ServerWorldEvents.LOAD.register { server, level ->
            DataRemoteSync.handleWorldLoaded(server, level)
            ResearcherDiaryComponent.Callbacks.onServerLevel(level)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            AsyncLocator.handleServerStoppingEvent()
            DataRemoteSync.handleServerStoppingEvent()
            GrowssethApiV1.Callbacks.onServerStop(server)
            ResearcherTrades.onServerStop(server)
            LiveUpdatesConnection.serverStop(server)
            GrowssethExtraEvents.onServerStop()
            CustomRemoteDiaries.onServerStopped()
            RemoteStructureBooks.onServerStopped()
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            LocationNotifListener.Callbacks.onServerStopped(server)
        }
        ServerTickEvents.START_SERVER_TICK.register { server ->
            DataRemoteSync.checkTickSync(WebConfig.dataSyncUrl, server)
            GrowssethAdvancements.Callbacks.onServerTick(server)
        }

        ServerTickEvents.END_WORLD_TICK.register { level ->
            level.players().forEach(::onServerPlayerTick)
        }

        ServerChunkEvents.CHUNK_LOAD.register { level, chunk ->
            GrowssethExtraEvents.Callbacks.onLoadChunk(level, chunk)
        }

        ServerEntityEvents.ENTITY_LOAD.register { entity, level ->
            QuestComponentEvents.onLoadEntity(entity)
        }
        ServerEntityEvents.ENTITY_UNLOAD.register { entity, level ->
//            QuestComponentEvents.onUnloadEntity(entity, level)
        }
        ServerEntityLifecycleEvents.ENTITY_DESTROYED.register { entity, level ->
            Researcher.Callbacks.onEntityDestroyed(entity, level)
        }

        PlayerBlockBreakEvents.AFTER.register { level, player, pos, state, entity ->
            ResearcherDialoguesComponent.Callbacks.onBlockBreak(level, player, pos, state, entity)
        }
        PlaceBlockEvent.AFTER.register { player, world, pos, placeContext, blockState, item ->
            ResearcherDialoguesComponent.Callbacks.onPlaceBlock(player, world, pos, placeContext, blockState, item)
        }

        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            ResearcherTrades.onServerPlayerJoin(handler, sender, server)
            GrowssethExtraEvents.onServerPlayerJoin(handler, sender, server)
            GrowssethWorldPreset.Callbacks.onServerPlayerJoin(handler, sender, server)
        }

        PlayerAdvancementEvent.EVENT.register { player, advancement, criterionString ->
            BasicDialoguesComponent.Callbacks.onAdvancement(player, advancement, criterionString)
        }

        LeashEvents.FENCE_LEASH.register { mob, pos, player ->
            Researcher.Callbacks.onFenceLeash(mob, pos, player)
        }
        LeashEvents.FENCE_UNLEASH.register { mob, pos ->
            Researcher.Callbacks.onFenceUnleash(mob, pos)
        }

        // Register singularly because returns
        NameTagRenameEvent.BEFORE.register(Researcher.Callbacks::nameTagRename)
        //AttackEntityCallback.EVENT.register(BasicDialoguesComponent.Callbacks::onAttack)

        DisableStructuresEvents.STRUCTURE_GENERATE.register { level, structure, _, _, _, _, _, _, _, _ ->
            StructureDisabler.Callbacks.shouldDisableStructure(structure, level)
        }
    }

    fun onServerPlayerTick(player: ServerPlayer) {
        JigsawPieceTrigger.Callbacks.onServerPlayerTick(player)
    }
}