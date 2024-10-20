package com.ruslan.growsseth.http

import com.filloax.fxlib.*
import com.filloax.fxlib.api.*
import com.filloax.fxlib.api.codec.mutableSetOf
import com.filloax.fxlib.api.entity.getPersistData
import com.filloax.fxlib.api.nbt.putIfAbsent
import com.filloax.fxlib.api.savedata.FxSavedData
import com.filloax.fxlib.api.codec.*
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.SpawnTimeTracker
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ResearcherDonkey
import com.ruslan.growsseth.entity.researcher.ResearcherQuestComponent
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import com.ruslan.growsseth.networking.CustomToastPacket
import com.ruslan.growsseth.utils.*
import com.ruslan.growsseth.utils.MixinHelpers
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.saveddata.SavedData
import kotlin.jvm.optionals.getOrNull

/**
 * Miscellaneous events that do not fit in another specific class
 */
object GrowssethExtraEvents {
    private val prefixToHandler = mapOf<String, (ApiEvent, MinecraftServer, GrowssethApi) -> Unit>(
        "toast" to ::handleCustomToast,
        "tpResearcher" to ::handleTeleportResearcher,
        "rmResearcher" to ::handleRemoveOldResearchers,
        "rmTent" to ::handleRemoveTent,
        "spawnResearcher" to ::handleSpawnResearcher,
        "rmTentWithGift" to ::handleRemoveTentWithGift,
        RemoteCommandExec.PREFIX to RemoteCommandExec::handleCommandExec,
    )

    private val prefixToJoinHandler = mapOf<String, (ApiEvent, ServerPlayer) -> Unit>(
        "toast" to ::handleCustomToastJoin,
    )

    var queuedTpResearcherEvent: ApiEvent? = null
        private set
    var queuedRemoveTentWithGiftEvent: ApiEvent? = null
        private set
    var shouldRunResearcherRemoveCheck: Boolean = false
        private set

    /* For every custom event that should run once at a pos and be cleared, using its id (aka name + some prefix)
    return true to clear it.
    If you need to redo something, use a new id in the api data that controls this.
    */
    private val doOnChunkLoad = mutableMapOf<String, (LevelChunk, ServerLevel) -> Boolean>()



    fun init() {
        GrowssethApi.current.subscribe { api, server ->
            api.events.forEach { event ->
                if (event.active) {
                    val prefix = event.name.split("/")[0]
                    prefixToHandler[prefix]?.let { it(event, server, api) }
                }
            }

            // Remove events not activated or present
            EventUtil.runWhenServerStarted(server, true) {
                val savedData = EventsSavedData.get(server)
                savedData.removeResearchersTimes.entries.removeIf { (id, time) ->
                    var found = api.events.firstOrNull {
                        "rm-${it.desc?.trim()}" == id
                    }
                    found == null || !found.active
                }
            }
        }
    }

    fun onServerPlayerJoin(handler: ServerGamePacketListenerImpl, sender: PacketSender, server: MinecraftServer) {
        val player = handler.player
        GrowssethApi.current.events.forEach { event ->
            if (event.active) {
                val prefix = event.name.split("/")[0]
                prefixToJoinHandler[prefix]?.let { it(event, player) }
            }
        }
    }

    fun onServerStop() {
        doOnChunkLoad.clear()
        queuedTpResearcherEvent = null
        queuedRemoveTentWithGiftEvent = null
        shouldRunResearcherRemoveCheck = false
    }

    // Custom toast

    private fun handleCustomToast(event: ApiEvent, server: MinecraftServer, api: GrowssethApi) {
        // Avoid doing too soon on init
        val tickCount = server.tickCount
        val minStartTime = 40
        val delay = (minStartTime - tickCount).coerceAtLeast(0)
        if (delay > 0) {
            // redo at the minimum time
            ScheduledServerTask.schedule(server, delay) {
                sendAllCustomToastEvent(event, server)
            }
        } else {
            sendAllCustomToastEvent(event, server)
        }
    }

    private fun getToastPacket(event: ApiEvent, server: MinecraftServer): CustomToastPacket  {
        val titlePart = event.name.replace("toast/", "").trim()
        val itemToastPattern = Regex("(?<namespace>[^/]+)/(?<path>[^/]+)/(?<title>.+)")
        val match = itemToastPattern.matchEntire(titlePart)
        var item: Item? = null
        val title: String = if (match != null) {
            val namespace = match.groups["namespace"]?.value
            val path = match.groups["path"]?.value
            val title = match.groups["title"]?.value
            if (namespace != null && path != null && title != null) {
                val itemId = ResourceLocation.fromNamespaceAndPath(namespace, path)
                val registryAccess = server.registryAccess()
                item = registryAccess.registry(Registries.ITEM).getOrNull()?.get(itemId)
                if (item == null) {
                    RuinsOfGrowsseth.LOGGER.warn("Custom item toast: couldn't find item with id $itemId")
                }
                title
            } else {
                titlePart
            }
        } else {
            titlePart
        }

        val content = event.desc?.let { Component.literal(it) }

        return CustomToastPacket(Component.literal(title), content, item?.defaultInstance ?: ItemStack.EMPTY)
    }

    private fun handleCustomToastJoin(event: ApiEvent, player: ServerPlayer) {
        val seqId = event.pos?.let { "${it.x}${it.y}${it.z}" } ?: ""
        val packet = getToastPacket(event, player.server)

        checkAndSendCustomToastEvent(player, packet, seqId)
    }

    private fun sendAllCustomToastEvent(event: ApiEvent, server: MinecraftServer) {
        val seqId = event.pos?.let { "${it.x}${it.y}${it.z}" } ?: ""
        val packet = getToastPacket(event, server)

        server.playerList.players.forEach { player -> checkAndSendCustomToastEvent(player, packet, seqId) }
    }

    private fun checkAndSendCustomToastEvent(player: ServerPlayer, packet: CustomToastPacket, seqId: String) {
        val data = player.getPersistData()
        data.putIfAbsent("growsseth:customToastMemory", CompoundTag())
        val memory = data.getCompound("growsseth:customToastMemory")
        val id = packet.title.string + seqId

        if (!memory.contains(id)) {
            ServerPlayNetworking.send(player, packet)
            memory.put(id, IntTag.valueOf(1))
        }
    }

    // Researcher move
    private fun handleTeleportResearcher(event: ApiEvent, server: MinecraftServer, api: GrowssethApi) {
        val pos = event.pos
        val id = event.desc?.let { "tp-$it" }
        if (pos == null || id == null) {
            RuinsOfGrowsseth.LOGGER.error("Teleport researcher event must have a position and desc! $event")
            return
        }

        // Run when started so level is loaded
        EventUtil.runWhenServerStarted(server, true) { srv ->
            val savedData = EventsSavedData.get(srv)
            if (savedData.alreadyRan.contains(id)) {
                return@runWhenServerStarted
            }

            if (queuedTpResearcherEvent != null) {
                RuinsOfGrowsseth.LOGGER.warn("Tp researcher already scheduled, overlap?")
            }
            queuedTpResearcherEvent = event
            RuinsOfGrowsseth.LOGGER.info("Scheduler researcher tp to $pos ($id)")
        }
    }

    fun teleportResearcher(researcher: Researcher, level: ServerLevel) {
        val event = queuedTpResearcherEvent ?: run {
            RuinsOfGrowsseth.LOGGER.warn("Tried to teleport researcher but no teleport action queued!")
            return
        }
        val id = event.desc?.let { "tp-$it" } ?: run {
            RuinsOfGrowsseth.LOGGER.error("No desc in teleport event $event")
            return
        }
        val pos = event.pos ?: run {
            RuinsOfGrowsseth.LOGGER.error("No pos in teleport event $event")
            return
        }
        val server = level.server
        val savedData = EventsSavedData.get(server)
        if (!savedData.alreadyRan.contains(id)) {
            val teleportPos = level.nearestFreePosition(pos, aboveSolid = true, onlyAbove = true) ?: pos
            researcher.moveTo(teleportPos.center)
            researcher.resetStartingPos(teleportPos)
            RuinsOfGrowsseth.LOGGER.info("Teleported researcher $researcher to $teleportPos[$pos] ($id)")

            savedData.alreadyRan.add(id)
            savedData.setDirty()
        } else {
            RuinsOfGrowsseth.LOGGER.warn("Tried to teleport researcher twice! (id: $id)")
        }
        queuedTpResearcherEvent = null
    }

    // Remove old researchers (counting gametime, minus 1 minute in case somewhere spawned in the meantime)
    private fun handleRemoveOldResearchers(event: ApiEvent, server: MinecraftServer, api: GrowssethApi) {
        val id = event.desc?.let { "rm-$it" }
        if (id == null) {
            RuinsOfGrowsseth.LOGGER.error("Remove researcher event must have a desc! $event")
            return
        }

        // Run when started so level is loaded
        EventUtil.runWhenServerStarted(server, true) {
            val savedData = EventsSavedData.get(server)
            if (savedData.removeResearchersTimes.containsKey(id)) {
                shouldRunResearcherRemoveCheck = true
                return@runWhenServerStarted
            }

            val time = server.overworld().gameTime - 60f.secondsToTicks()
            savedData.removeResearchersTimes[id] = time
            savedData.setDirty()
            RuinsOfGrowsseth.LOGGER.info("Set researchers older than $time (${time.ticksToTimecode()}) to be removed")

            // Setup logic here, so only runs if needed
            shouldRunResearcherRemoveCheck = true
        }
    }

    fun researcherRemoveCheck(entity: LivingEntity, tracker: SpawnTimeTracker) {
        if (entity.level().isClientSide) return
        val savedData = EventsSavedData.get(entity.level().server!!)
        if (savedData.removeResearchersTimes.isEmpty()) return
        val maxTime = savedData.removeResearchersTimes.values.max()

        if (tracker.spawnTime < maxTime) {
            entity.discard()
            RuinsOfGrowsseth.LOGGER.info("Removed researcher $entity via remote command (time was ${tracker.spawnTime} ${tracker.spawnTime.ticksToTimecode()})")
        }
    }

    // Remove tent and donkey, donkey identity obtained from world data
    // so assumes the default shared id for world data of researchers aka 0

    fun handleRemoveTent(event: ApiEvent, server: MinecraftServer, api: GrowssethApi) {
        val id = event.desc?.let { "rmTent-$it" }
        val pos = event.pos
        if (pos == null || id == null) {
            RuinsOfGrowsseth.LOGGER.error("Remove tent event must have a position and desc! $event")
            return
        }
        val chunkPos = ChunkPos(pos)

        // Run when started so level is loaded
        EventUtil.runWhenServerStarted(server, true) {
            val savedData = EventsSavedData.get(server)
            if (savedData.alreadyRan.contains(id)) {
                return@runWhenServerStarted
            }

            EventUtil.runWhenChunkLoaded(server.overworld(), chunkPos) { level ->
                if (savedData.alreadyRan.contains(id)) {
                    return@runWhenChunkLoaded
                }

                val structureManager = level.structureManager()
                // Use MixinHelpers as this is debug stuff and no worth adding yet another tent Structure reference
                var tent = structureManager.getStructureAt(pos, MixinHelpers.researcherTent!!)
                if (!tent.isValid) {
                    val defaultY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.x, pos.z) - 10
                    tent = structureManager.getStructureAt(BlockPos(pos.x, defaultY, pos.z), MixinHelpers.researcherTent!!)
                }
                if (!tent.isValid) {
                    RuinsOfGrowsseth.LOGGER.warn("Cannot remove tent at $pos as no tent there!")
                    return@runWhenChunkLoaded
                }
                tent.pieces.forEach { piece ->
                    if (piece is ResearcherTent) {
                        piece.remove(level, replaceUndergroundEntrance = true)

                        // Remove donkey but only if close to tent
                        ResearcherDonkey.removeDonkey(piece, server.overworld()) {
                            it.position().distanceTo(tent.boundingBox.center.center) < 40.0
                        }
                    }
                }

                savedData.alreadyRan.add(id)
                savedData.setDirty()
            }
        }
    }

    fun handleRemoveTentWithGift(event: ApiEvent, server: MinecraftServer, api: GrowssethApi) {
        val id = event.desc?.let { "rmTentGift-$it" }
        if (id == null) {
            RuinsOfGrowsseth.LOGGER.error("Remove tent with gift event must have a desc! $event")
            return
        }

        // Run when started so level is loaded
        EventUtil.runWhenServerStarted(server, true) { srv ->
            val savedData = EventsSavedData.get(srv)
            if (savedData.alreadyRan.contains(id)) {
                return@runWhenServerStarted
            }

            if (queuedRemoveTentWithGiftEvent != null) {
                RuinsOfGrowsseth.LOGGER.warn("Remove tent with gift already scheduled, overlap?")
            }
            queuedRemoveTentWithGiftEvent = event
            RuinsOfGrowsseth.LOGGER.info("Scheduler remove tent with gift")
        }
        queuedRemoveTentWithGiftEvent = null
    }

    fun removeTentWithGift(researcher: Researcher, level: ServerLevel) {
        val event = queuedRemoveTentWithGiftEvent ?: run {
            RuinsOfGrowsseth.LOGGER.warn("Tried to remove tent with gift but no event queued!")
            return
        }
        val id = event.desc?.let { "rmTentGift-$it" } ?: run {
            RuinsOfGrowsseth.LOGGER.error("No desc in remove tent with gift event $event")
            return
        }

        val server = level.server
        val savedData = EventsSavedData.get(server)
        if (!savedData.alreadyRan.contains(id)) {
            ResearcherQuestComponent.removeTentAndResearcher(researcher)
            savedData.alreadyRan.add(id)
            savedData.setDirty()
        } else {
            RuinsOfGrowsseth.LOGGER.warn("Tried to remove tent with gift twice! (id: $id)")
        }
    }

    // Spawn researcher
    // Spawns a new researcher, without tent

    fun handleSpawnResearcher(event: ApiEvent, server: MinecraftServer, api: GrowssethApi) {
        val id = event.desc?.let { "spawnRes-$it" }
        val pos = event.pos
        if (pos == null || id == null) {
            RuinsOfGrowsseth.LOGGER.error("spawn Researcher event must have a position and desc! $event")
            return
        }
        val chunkPos = ChunkPos(pos)

        // Run when started so level is loaded
        EventUtil.runWhenServerStarted(server, true) {
            val savedData = EventsSavedData.get(server)
            if (savedData.alreadyRan.contains(id)) {
                return@runWhenServerStarted
            }

            EventUtil.runWhenChunkLoaded(server.overworld(), chunkPos) { level ->
                if (savedData.alreadyRan.contains(id)) {
                    return@runWhenChunkLoaded
                }
                savedData.alreadyRan.add(id)
                savedData.setDirty()

                val mob = GrowssethEntities.RESEARCHER.create(level) ?: run {
                    RuinsOfGrowsseth.LOGGER.error("Couldn't spawn researcher through growsseth event!")
                    return@runWhenChunkLoaded
                }
                mob.moveTo(pos.x + .5, pos.y + .0, pos.z + .5, 0.0f, 0.0f)
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null)
                level.addFreshEntityWithPassengers(mob)
            }
        }
    }

    object Callbacks {
        fun onLoadChunk(level: ServerLevel, chunk: LevelChunk) {
            val savedData: EventsSavedData by lazy {
                EventsSavedData.get(level.server)
            }

            doOnChunkLoad.entries.removeIf { (id, action) ->
                if (savedData.alreadyRan.contains(id))
                    return@removeIf false

                val clear = action(chunk, level)
                if (clear) {
                    savedData.alreadyRan.add(id)
                    savedData.setDirty()
                }
                clear
            }
        }
    }

    class EventsSavedData private constructor(
        alreadyRan: Set<String> = setOf(),
        removeResearchersTimes: Map<String, Long> = mapOf(),
    ) : FxSavedData<EventsSavedData>(CODEC) {
        val alreadyRan: MutableSet<String> = alreadyRan.toMutableSet()
        val removeResearchersTimes: MutableMap<String, Long> = removeResearchersTimes.toMutableMap()

        companion object {
            val CODEC: Codec<EventsSavedData> = RecordCodecBuilder.create { builder -> builder.group(
                Codec.STRING.mutableSetOf().fieldOf("alreadyRan").forGetter(EventsSavedData::alreadyRan),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("removeResearchersTimes").forGetter(EventsSavedData::removeResearchersTimes),
            ).apply(builder, ::EventsSavedData) }
            private val DEF = define("growssethEvents", ::EventsSavedData, CODEC)

            fun get(server: MinecraftServer): EventsSavedData {
                return server.loadData(DEF)
            }
        }
    }
}