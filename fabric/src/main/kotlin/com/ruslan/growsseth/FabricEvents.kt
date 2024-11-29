package com.ruslan.growsseth

import com.filloax.fxlib.platform.ServerEvent
import com.ruslan.growsseth.loot.LootTableModifier
import net.fabricmc.fabric.api.event.lifecycle.v1.*
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk

class FabricEvents : ModEvents() {
    override fun onServerStarting(event: ServerEvent) = ServerLifecycleEvents.SERVER_STARTING.register(event)

    override fun onServerStarted(event: ServerEvent) = ServerLifecycleEvents.SERVER_STARTED.register(event)

    override fun onServerStopping(event: ServerEvent) = ServerLifecycleEvents.SERVER_STOPPING.register(event)

    override fun onServerStopped(event: ServerEvent) = ServerLifecycleEvents.SERVER_STOPPED.register(event)

    override fun onServerLevelLoad(event: (MinecraftServer, ServerLevel) -> Unit) = ServerWorldEvents.LOAD.register(event)

    override fun onStartServerTick(event: ServerEvent) = ServerTickEvents.START_SERVER_TICK.register(event)

    override fun onEndServerLevelTick(event: (ServerLevel) -> Unit) = ServerTickEvents.END_WORLD_TICK.register(event)

    override fun onLoadChunk(event: (level: ServerLevel, chunk: LevelChunk) -> Unit) = ServerChunkEvents.CHUNK_LOAD.register(event)

    override fun onEntityLoad(event: (entity: Entity, level: ServerLevel) -> Unit) = ServerEntityEvents.ENTITY_LOAD.register(event)

    override fun onEntityUnload(event: (entity: Entity, level: ServerLevel) -> Unit) = ServerEntityEvents.ENTITY_UNLOAD.register(event)

    override fun afterPlayerBlockBreak(event: (Level, Player, BlockPos, BlockState) -> Unit) = PlayerBlockBreakEvents.AFTER.register { level, player, pos, state, _ ->
        event(level, player, pos, state)
    }

    override fun onPlayerServerJoin(event: (ServerPlayer, MinecraftServer) -> Unit) = ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
        event(handler.player, server)
    }

    override fun onLootTableModify(event: (key: ResourceLocation, table: LootTableModifier) -> Unit) = LootTableEvents.MODIFY.register { key, tableBuilder, _, registries ->
        event(key.location(), LootTableModifier.ForLootTableBuilder(tableBuilder))
    }
}