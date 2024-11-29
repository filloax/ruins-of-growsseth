package com.ruslan.growsseth

import com.filloax.fxlib.platform.ServerEvent
import com.ruslan.growsseth.loot.LootTableModifier
import com.ruslan.growsseth.loot.LootTableModifierNeo
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
import net.minecraft.world.level.storage.loot.LootTable
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.LootTableLoadEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent
import net.neoforged.neoforge.event.level.ChunkEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

class NeoEvents : ModEvents() {
    override fun onServerStarting(event: ServerEvent) {
        NeoForge.EVENT_BUS.addListener { ev: ServerStartingEvent ->
            event(ev.server)
        }
    }

    override fun onServerStarted(event: ServerEvent) {
        NeoForge.EVENT_BUS.addListener { ev: ServerStartedEvent ->
            event(ev.server)
        }
    }

    override fun onServerStopping(event: ServerEvent) {
        NeoForge.EVENT_BUS.addListener { ev: ServerStoppingEvent ->
            event(ev.server)
        }
    }

    override fun onServerStopped(event: ServerEvent) {
        NeoForge.EVENT_BUS.addListener { ev: ServerStoppedEvent ->
            event(ev.server)
        }
    }

    override fun onServerLevelLoad(event: (MinecraftServer, ServerLevel) -> Unit) {
        NeoForge.EVENT_BUS.addListener { ev: LevelEvent.Load ->
            if (!ev.level.isClientSide) {
                event(ev.level.server!!, ev.level as ServerLevel)
            }
        }
    }

    override fun onStartServerTick(event: ServerEvent) {
        NeoForge.EVENT_BUS.addListener { ev: ServerTickEvent.Pre ->
            event(ev.server)
        }
    }

    override fun onEndServerLevelTick(event: (ServerLevel) -> Unit) {
        NeoForge.EVENT_BUS.addListener { ev: LevelTickEvent.Post ->
            if (!ev.level.isClientSide) {
                event(ev.level as ServerLevel)
            }
        }
    }

    override fun onLoadChunk(event: (level: ServerLevel, chunk: LevelChunk) -> Unit) {
        NeoForge.EVENT_BUS.addListener { ev: ChunkEvent.Load ->
            if (ev.level is ServerLevel && ev.chunk is LevelChunk) {
                event(ev.level as ServerLevel, ev.chunk as LevelChunk)
            }
        }
    }

    override fun onEntityLoad(event: (entity: Entity, level: ServerLevel) -> Unit) {
        NeoForge.EVENT_BUS.addListener { ev: EntityJoinLevelEvent ->
            if (ev.level is ServerLevel) {
                event(ev.entity, ev.level as ServerLevel)
            }
        }
    }

    override fun onEntityUnload(event: (entity: Entity, level: ServerLevel) -> Unit) {
        NeoForge.EVENT_BUS.addListener { ev: EntityLeaveLevelEvent ->
            if (ev.level is ServerLevel) {
                event(ev.entity, ev.level as ServerLevel)
            }
        }
    }

    override fun afterPlayerBlockBreak(event: (Level, Player, BlockPos, BlockState) -> Unit) {
        NeoForge.EVENT_BUS.addListener { ev: BreakEvent ->
            event(ev.level as Level, ev.player, ev.pos, ev.state)
        }
    }

    override fun onPlayerServerJoin(event: (player: ServerPlayer, MinecraftServer) -> Unit) {
        NeoForge.EVENT_BUS.addListener { ev: PlayerLoggedInEvent ->
            val player = ev.entity
            if (player is ServerPlayer) {
                event(player, player.server)
            }
        }
    }

    override fun onLootTableModify(event: (key: ResourceLocation, table: LootTableModifier) -> Unit) {
        NeoForge.EVENT_BUS.addListener { ev: LootTableLoadEvent ->
            event(ev.name, LootTableModifierNeo(ev.table))
        }
    }
}