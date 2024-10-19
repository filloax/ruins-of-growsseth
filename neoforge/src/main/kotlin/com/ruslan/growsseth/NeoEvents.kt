package com.ruslan.growsseth

import com.filloax.fxlib.platform.ServerEvent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.storage.loot.LootTable

class NeoEvents : ModEvents() {
    override fun onServerStarting(event: ServerEvent) {
        TODO("Not yet implemented")
    }

    override fun onServerStarted(event: ServerEvent) {
        TODO("Not yet implemented")
    }

    override fun onServerStopping(event: ServerEvent) {
        TODO("Not yet implemented")
    }

    override fun onServerStopped(event: ServerEvent) {
        TODO("Not yet implemented")
    }

    override fun onServerLevelLoad(event: (MinecraftServer, ServerLevel) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun onStartServerTick(event: ServerEvent) {
        TODO("Not yet implemented")
    }

    override fun onEndServerLevelTick(event: (ServerLevel) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun onLoadChunk(event: (level: ServerLevel, chunk: LevelChunk) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun onEntityLoad(event: (entity: Entity, level: ServerLevel) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun onEntityUnload(event: (entity: Entity, level: ServerLevel) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun afterPlayerBlockBreak(event: (Level, Player, BlockPos, BlockState, BlockEntity?) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun onPlayerServerJoin(event: (handler: ServerGamePacketListenerImpl, MinecraftServer) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun onLootTableModify(event: (key: ResourceKey<LootTable>, tableBuilder: LootTable.Builder, registries: HolderLookup.Provider) -> Unit) {
        TODO("Not yet implemented")
    }
}