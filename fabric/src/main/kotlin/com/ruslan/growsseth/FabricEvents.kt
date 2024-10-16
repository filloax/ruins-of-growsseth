package com.ruslan.growsseth

import com.filloax.fxlib.platform.ServerEvent
import com.ruslan.growsseth.events.*
import net.fabricmc.fabric.api.event.lifecycle.v1.*
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.loot.v3.LootTableSource
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Leashable
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.storage.loot.LootTable

object FabricEvents : ModEvents() {
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

    override fun afterPlayerBlockBreak(event: (Level, Player, BlockPos, BlockState, BlockEntity?) -> Unit) = PlayerBlockBreakEvents.AFTER.register(event)

    override fun onPlayerServerJoin(event: (handler: ServerGamePacketListenerImpl, MinecraftServer) -> Unit) = ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
        event(handler, server)
    }

    override fun onLootTableModify(event: (key: ResourceKey<LootTable>, tableBuilder: LootTable.Builder, registries: HolderLookup.Provider) -> Unit) = LootTableEvents.MODIFY.register { key, tableBuilder, _, registries ->
        event(key, tableBuilder, registries)
    }
}