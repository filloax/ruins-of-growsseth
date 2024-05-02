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
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
import net.fabricmc.fabric.api.loot.v2.LootTableSource
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
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
import net.minecraft.world.InteractionResult
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

    override fun onEntityDestroyed(event: (entity: Entity, level: ServerLevel) -> Unit) = ServerEntityLifecycleEvents.ENTITY_DESTROYED.register(event)

    override fun afterPlayerBlockBreak(event: (Level, Player, BlockPos, BlockState, BlockEntity?) -> Unit) = PlayerBlockBreakEvents.AFTER.register(event)

    override fun afterPlayerPlaceBlock(event: (Player, Level, BlockPos, BlockPlaceContext, BlockState, BlockItem) -> Unit) = PlaceBlockEvent.AFTER.register(event)

    override fun onPlayerServerJoin(event: (handler: ServerGamePacketListenerImpl, PacketSender, MinecraftServer) -> Unit) = ServerPlayConnectionEvents.JOIN.register(event)

    override fun onPlayerAdvancement(event: (ServerPlayer, AdvancementHolder, criterionString: String) -> Unit) = PlayerAdvancementEvent.EVENT.register(event)

    override fun onFenceLeash(event: (Mob, BlockPos, ServerPlayer) -> Unit) = LeashEvents.FENCE_LEASH.register(event)

    override fun onFenceUnleash(event: (Mob, BlockPos) -> Unit) = LeashEvents.FENCE_UNLEASH.register(event)

    override fun beforeNameTagRename(event: (target: LivingEntity, Component, ServerPlayer, ItemStack, InteractionHand) -> InteractionResultHolder<ItemStack>) = NameTagRenameEvent.BEFORE.register(event)

    override fun onLootTableModify(event: (resourceManager: ResourceManager, lootManager: LootDataManager, id: ResourceLocation, tableBuilder: LootTable.Builder, source: LootTableSource) -> Unit) = LootTableEvents.MODIFY.register(event)
}