package com.ruslan.growsseth.dialogues

import com.filloax.fxlib.api.codec.mapWithValueOf
import com.filloax.fxlib.api.codec.mutableMapCodec
import com.filloax.fxlib.api.codec.mutableSetOf
import com.filloax.fxlib.api.nbt.loadField
import com.filloax.fxlib.api.nbt.saveField
import com.filloax.fxlib.api.optional
import com.filloax.fxlib.api.secondsToTicks
import com.filloax.fxlib.api.weightedRandom
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ClientConfig
import com.ruslan.growsseth.config.MiscConfig
import com.ruslan.growsseth.dialogues.BasicDialogueEvents
import com.ruslan.growsseth.dialogues.DialoguesNpc.Companion.getDialogueNpcs
import com.ruslan.growsseth.networking.DialoguePacket
import com.ruslan.growsseth.quests.QuestOwner
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.phys.AABB
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import kotlin.jvm.optionals.getOrNull
import com.ruslan.growsseth.dialogues.BasicDialogueEvents as Events
import org.apache.logging.log4j.Level as LogLevel

// Server side only
open class BasicDialoguesComponent(
    val entity: LivingEntity, val random: RandomSource,
) : NpcDialoguesComponent {
    companion object {
        val PLAYER_DATA_CODEC: Codec<PlayerData> = RecordCodecBuilder.create { builder -> builder.group(
            Codec.STRING.mapWithValueOf(Codec.INT).fieldOf("dialogueCount").forGetter(PlayerData::dialogueCount),
            DialogueEvent.CODEC.mapWithValueOf(Codec.INT).fieldOf("eventTriggerCount").forGetter(PlayerData::eventTriggerCount),
            DialogueEvent.CODEC.mapWithValueOf(Codec.INT).fieldOf("eventCloseTriggerCount").forGetter(PlayerData::eventCloseTriggerCount),
            DialogueEvent.CODEC.mapWithValueOf(Codec.LONG).fieldOf("eventLastTriggerTime").forGetter(PlayerData::eventLastTriggerTime),
            Codec.LONG.optionalFieldOf("lastSeenTimestamp").forGetter(PlayerData::lastSeenTimestamp.optional()),
            Codec.LONG.optionalFieldOf("lastArrivedTimestamp").forGetter(PlayerData::lastArrivedTimestamp.optional()),
        ).apply(builder) { d, et, ec, el, ls, la ->  PlayerData(d, et, ec, el, ls.getOrNull(), la.getOrNull()) } }

        val TARGETING: TargetingConditions = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting()
    }

    data class PlayerData(
        val dialogueCount: MutableMap<String, Int> = mutableMapOf(),
        val eventTriggerCount: MutableMap<DialogueEvent, Int> = mutableMapOf(),
        val eventCloseTriggerCount: MutableMap<DialogueEvent, Int> = mutableMapOf(),
        val eventLastTriggerTime: MutableMap<DialogueEvent, Long> = mutableMapOf(),
        var lastSeenTimestamp: Long? = null,
        var lastArrivedTimestamp: Long? = null,
        // Do not persist, not necessary
        val lastEventDialogue: MutableMap<DialogueEvent, DialogueEntry> = mutableMapOf(),
    )

    // NBT data
    // First key is player UUID
    protected val closePlayers = mutableSetOf<UUID>()
    protected val leavingPlayers = mutableMapOf<UUID, Int>()
    protected val playersArrivedSoon = mutableMapOf<UUID, Boolean>()
    protected val savedPlayersData = mutableMapOf<UUID, PlayerData>()

    // UUID is player's
    protected val dialogueQueues = mutableMapOf<UUID, Deque<Pair<DialogueLine, DialogueEvent>>>()
    protected var dialogueQueueDelays = mutableMapOf<UUID, Int>()
    protected val serverLevel: ServerLevel get() = entity.level() as ServerLevel
    protected val server get() = serverLevel.server

    open var nearbyRadius = 12.0
    open var radiusForTriggerLeave = 17.0
    open var maxDialogueRadius = 50.0 // Radius before any dialogue event is prevented from triggering
    open var secondsForTriggerLeave = 0   // waiting time before saying goodbye when a player leaves
    open var checkLineOfSight = true
    /** Set to 0 to disable "soon" events: */
    open var secondsForArriveSoon = 10
    /** Set to 0 to disable "long time" events: */
    open var secondsForArriveLongTime = 6 * 3600 // 6 hours
    open var secondsForCloseRepeat = 60
    open var secondsForAttackRepeat = 10
    open var maxCloseHitsForDialogues = 3
    /** Set to 0 to have no wait: */
    open var dialogueDelayMaxSeconds = 0.6f
    open var dialogueSecondsSameId = .1f // + estimatedReadingTime

    protected fun playerDataOrCreate(player: ServerPlayer) = playerDataOrCreate(player.uuid)
    protected fun playerDataOrCreate(uuid: UUID) = savedPlayersData.computeIfAbsent(uuid) { PlayerData() }
    protected fun playerData(player: ServerPlayer) = playerData(player.uuid)
    protected fun playerData(uuid: UUID) = savedPlayersData[uuid]


    open fun onPlayerArrive(player: ServerPlayer) {
        triggerDialogue(player, Events.PLAYER_ARRIVE_LONG_TIME, Events.PLAYER_ARRIVE_SOON, Events.PLAYER_ARRIVE_NIGHT, Events.PLAYER_ARRIVE)
    }

    open fun onPlayerLeave(player: ServerPlayer) {
        playerDataOrCreate(player).lastSeenTimestamp = entity.level().gameTime
        triggerDialogue(player, Events.PLAYER_LEAVE_SOON, Events.PLAYER_LEAVE_NIGHT, Events.PLAYER_LEAVE)
    }

    override fun resetNearbyPlayers() {
        closePlayers.toList().forEach { uuid ->
            playerDataOrCreate(uuid).lastSeenTimestamp = entity.level().gameTime
        }
        closePlayers.clear()
    }

    /**
     * If not overridden, only handles events in BasicDialogueEvents otherwise returns true
     */
    override fun canTriggeredEventRun(player: ServerPlayer, dialogueEvent: DialogueEvent): Boolean {
        return when (dialogueEvent) {
            Events.PLAYER_ARRIVE_NIGHT -> entity.level().isNight
            Events.PLAYER_ARRIVE_SOON -> secondsForArriveSoon > 0
                    && playerData(player)?.lastSeenTimestamp?.let { getSecondsSinceWorldTime(it) < secondsForArriveSoon } == true
            Events.PLAYER_ARRIVE_LONG_TIME ->  secondsForArriveLongTime > 0
                    && playerData(player)?.lastSeenTimestamp?.let { getSecondsSinceWorldTime(it) > secondsForArriveLongTime } == true
            Events.PLAYER_LEAVE_NIGHT -> entity.level().isNight
            Events.PLAYER_LEAVE_SOON -> secondsForArriveSoon > 0
                    && playersArrivedSoon[player.uuid] ?: false
            Events.HIT_BY_PLAYER -> secondsForAttackRepeat > 0
            Events.RENAME -> !eventInQueue(dialogueEvent)
            else -> true
        }
    }

    override fun dialoguesStep() {
        // Run every 2 ticks for better performance
        if (entity.tickCount % 2 == 0) {
            checkNearbyPlayers()
        }

        for ((playerUuid, dialogueQueue) in dialogueQueues) {
            var dialogueQueueDelay = dialogueQueueDelays.computeIfAbsent(playerUuid) { 0 }
            val player = server.playerList.getPlayer(playerUuid)
            if (player == null) {
                RuinsOfGrowsseth.LOGGER.warn("Player $playerUuid left while dialogues were still queued!")
                dialogueQueue.clear()
                continue
            }
            if (dialogueQueue.isNotEmpty()) {
                dialogueQueueDelay--
                if (dialogueQueueDelay <= 0) {
                    val (line, _) = dialogueQueue.remove()
                    sendDialogueToPlayer(player, line)
                    if (dialogueQueue.isNotEmpty()) {
                        val sameId = line.dialogue.id == dialogueQueue.peek().first.dialogue.id
                        dialogueQueueDelay = if (line.duration != null) {
                            line.duration.secondsToTicks()
                        } else if (sameId) {
                            val readingTime = if (MiscConfig.dialogueWordsPerMinute > 0) estimateReadingTime(line.content) else 0F
                            (dialogueSecondsSameId + readingTime).secondsToTicks()
                        } else {
                            // Shorter delay in consecutive dialogues
                            if (dialogueDelayMaxSeconds > 0)
                                random.nextInt() % (dialogueDelayMaxSeconds / 2).secondsToTicks()
                            else 0
                        }
                        if (!sameId)
                            sendSeparatorToPlayer(player)
                    } else {
                        sendSeparatorToPlayer(player)
                    }
                }
            }
            dialogueQueueDelays[playerUuid] = dialogueQueueDelay
        }
    }

    private fun checkNearbyPlayers() {
        val detectRadius = radiusForTriggerLeave
        val visiblePlayers: Set<ServerPlayer> = serverLevel.getNearbyPlayers(
            TARGETING,
            entity,
            AABB.ofSize(entity.position(), detectRadius, detectRadius, detectRadius)
        ).map { it as ServerPlayer }.toSet()
        val knownPlayers = closePlayers.mapNotNull { serverLevel.getPlayerByUUID(it) as ServerPlayer? }
        val possiblePlayers = visiblePlayers + knownPlayers

        // Bounding box inside of which players are considered "near" (triggering hello dialogue)
        val nearbyBoundingBox = AABB.ofSize(entity.position(), nearbyRadius, nearbyRadius, nearbyRadius)
        // Bounding box outside of which players are considered "far" (triggering goodbye dialogue)
        val farBoundingBox = AABB.ofSize(entity.position(), radiusForTriggerLeave, nearbyRadius, radiusForTriggerLeave)

        // Players close enough to trigger arrive
        val nearPlayers = possiblePlayers.filter {
            nearbyBoundingBox.contains(it.position())
            && (!checkLineOfSight || entity.hasLineOfSight(it))
        }.toMutableSet()
        // Players far enough to trigger leave
        val farPlayers = possiblePlayers.filterNot { farBoundingBox.contains(it.position()) }.toMutableSet()
        changeNearPlayers(nearPlayers, farPlayers)

        // Player not in either area
        val inbetweenPlayers = possiblePlayers - nearPlayers - farPlayers

        for (player in nearPlayers) {
            if (player.uuid !in closePlayers) {
                closePlayers.add(player.uuid)
                onPlayerArrive(player)
            }
            if (player.uuid in leavingPlayers) {
                leavingPlayers.remove(player.uuid)
            }
            triggerDialogueInternal(player, Events.TICK_NEAR_PLAYER, ignoreEmptyOptionsWarning = true, countEvents = false)
        }
        // Stop leaving players triggering leave if inbetween the areas
        for (player in inbetweenPlayers) {
            if (player.uuid in leavingPlayers) {
                leavingPlayers.remove(player.uuid)
            }
        }
        for (player in farPlayers) {
            if (player.uuid in closePlayers) {
                val tickCount = leavingPlayers.computeIfAbsent(player.uuid) { player.tickCount }
                if (getSecondsSinceTick(player, tickCount) >= secondsForTriggerLeave) {
                    onPlayerLeave(player)
                    closePlayers.remove(player.uuid)
                    leavingPlayers.remove(player.uuid)
                }
            }
        }

        afterPlayersCheck(nearPlayers, inbetweenPlayers, farPlayers)
    }

    /**
     * Change if a player is considered near or far after distance detection (for instance, checking inside structures, etc)
     */
    protected open fun changeNearPlayers(nearPlayers: MutableSet<ServerPlayer>, farPlayers: MutableSet<ServerPlayer>) {}
    protected open fun afterPlayersCheck(nearPlayers: Set<ServerPlayer>, inbetweenPlayers: Set<ServerPlayer>, farPlayers: Set<ServerPlayer>) {}

    override fun sendDialogueToPlayer(player: ServerPlayer, line: DialogueLine) {
        if (line.content != "") {
//            val nameComp = Component.literal("<").append(entity.name.copy().withStyle(ChatFormatting.YELLOW)).append("> ")
//            val messageComp = nameComp.append(Component.translatable(line.content))
//            player.displayClientMessage(messageComp, false)
            ServerPlayNetworking.send(player, DialoguePacket(line, entity.name))
        }
    }

    protected open fun sendSeparatorToPlayer(player: ServerPlayer) {
        if (!ClientConfig.disableNpcDialogues) {
            val messageComp = Component.literal("*-------------------").withStyle(ChatFormatting.DARK_GRAY)
            player.displayClientMessage(messageComp, false)
        }
    }

    override fun triggerDialogueEntry(player: ServerPlayer, dialogueEntry: DialogueEntry) {
        queueDialogue(player, dialogueEntry, BasicDialogueEvents.MANUAL_TRIGGER)
    }

    protected fun queueDialogue(player: ServerPlayer, dialogueEntry: DialogueEntry, event: DialogueEvent) {
        if (dialogueEntry.content.isEmpty() || dialogueEntry.content[0].content == "") return

        val dialogueQueue = dialogueQueues.computeIfAbsent(player.uuid) { LinkedBlockingDeque() }

        if (dialogueDelayMaxSeconds > 0) {
            val lines = dialogueEntry.content
            if (dialogueEntry.immediate) {
                // Reverse so that in offering first segments are in right order
                lines.reversed().forEachIndexed{ idx, it ->
                    // reversed, so last is first
                    if (idx == lines.size - 1) {
                        // If queue is not empty (aka ongoing dialogue) send separator at start
                        // to distinguish it from other dialogues
                        if (dialogueQueue.isNotEmpty()) {
                            sendSeparatorToPlayer(player)
                        }
                        sendDialogueToPlayer(player, it)
                        if (lines.size == 1) {
                            sendSeparatorToPlayer(player)
                        }
                    } else {
                        dialogueQueue.offerFirst(Pair(it, event))
                        // Since first dialogue is played immediately, delay the second
                        if (idx == lines.size - 2 && MiscConfig.dialogueWordsPerMinute > 0) {
                            val readingTime = estimateReadingTime(lines[0].content)
                            dialogueQueueDelays[player.uuid] = (dialogueSecondsSameId + readingTime).secondsToTicks()
                        }
                    }
                }
            } else {
                lines.forEach { dialogueQueue.offer(Pair(it, event)) }
            }
            if ((dialogueQueueDelays[player.uuid] ?: 0) <= 0) {
                dialogueQueueDelays[player.uuid] = random.nextInt() % dialogueDelayMaxSeconds.secondsToTicks()
            }
        } else {
            dialogueEntry.content.forEach {
                sendDialogueToPlayer(player, it)
            }
        }
    }

    /**
     * Runs a random dialogue, using [canTriggeredEventRun] to check if it can run.
     * (devs: make it respect this contract)
     * @param dialogueEvents Dialogue events to get dialogue for, in order of priority
     * @param eventParam Parameter for the event to use in filtering dialogues
     * @param ignoreEventConditions if true: do not run checks with [canTriggeredEventRun]
     * @return If a dialogue was triggered
     * (will use next ones if previous are empty)
     */
    override fun triggerDialogue(
        player: ServerPlayer,
        vararg dialogueEvents: DialogueEvent,
        eventParam: String?,
        ignoreEventConditions: Boolean,
    ) : Boolean {
        return triggerDialogueInternal(player, *dialogueEvents, eventParam=eventParam, ignoreEventConditions=ignoreEventConditions)
    }

    /**
     * Implementation of [triggerDialogue] with extra options to
     * allow more control internally in this class
     */
    private fun triggerDialogueInternal(
        player: ServerPlayer,
        vararg dialogueEvents: DialogueEvent,
        eventParam: String? = null,
        ignoreEventConditions: Boolean = false,
        ignoreEmptyOptionsWarning: Boolean = false,
        countEvents: Boolean = true,
    ) : Boolean {
        val (event, dialogueOptions) = getDialoguesAndEvent(player, dialogueEvents, ignoreEventConditions, ignoreEmptyOptionsWarning) ?: return false

        if (countEvents && event.count) {
            incrementEventCount(event, player)
        }

        val validOptions = filterDialogueOptions(dialogueOptions, player, event, eventParam)
        val success = validOptions.isNotEmpty()

        onEventSelected(event, eventParam, player, success)

        if (!success) {
            if (!ignoreEmptyOptionsWarning && !dialogueEvents.any{it.ignoreNoDialogueWarning}) {
                RuinsOfGrowsseth.LOGGER.warn("No valid dialogue options for $event (param=$eventParam) $entity")
            }
            return false
        }

        val selected = validOptions.weightedRandom(DialogueEntry::weight::get, random)

        val playerData = playerDataOrCreate(player)
        playerData.lastEventDialogue[event] = selected

        if (selected.id != null) {
            playerData.dialogueCount.let{map -> map[selected.id] = map.getOrDefault(selected.id, 0) + 1 }
        }

        onDialogueSelected(selected, event, eventParam, player)

        queueDialogue(player, selected, event)
        return true
    }

    protected open fun incrementEventCount(event: DialogueEvent, player: ServerPlayer) {
        //to avoid repetitions
        val time =  entity.level().gameTime
        val pdata = playerDataOrCreate(player)
        val lastTriggerTime = pdata.eventLastTriggerTime[event]

        if (event == Events.HIT_BY_PLAYER && lastTriggerTime != null) {
            val count = pdata.eventCloseTriggerCount[event] ?: 1
            val secondsSinceLastAttack = getSecondsSinceWorldTime(lastTriggerTime)
            if (secondsSinceLastAttack < secondsForAttackRepeat && count < maxCloseHitsForDialogues)
                pdata.eventCloseTriggerCount[event] = count + 1
            else if (secondsSinceLastAttack > secondsForAttackRepeat * 2)
                pdata.eventCloseTriggerCount[event] = 1
        }
        else if (lastTriggerTime != null) {
            val count = pdata.eventCloseTriggerCount[event] ?: 1
            if (getSecondsSinceWorldTime(lastTriggerTime) < secondsForCloseRepeat) {
                pdata.eventCloseTriggerCount[event] = count + 1
            } else {
                pdata.eventCloseTriggerCount[event] = 1
            }
        } else {
            pdata.eventCloseTriggerCount[event] = 1
        }
        pdata.eventLastTriggerTime[event] = time

        pdata.eventTriggerCount[event] = pdata.eventTriggerCount.getOrDefault(event, 0) + 1

        // Handle special event behavior in close repetition
        when (event) {
            BasicDialogueEvents.PLAYER_ARRIVE, BasicDialogueEvents.PLAYER_ARRIVE_NIGHT, BasicDialogueEvents.PLAYER_ARRIVE_LONG_TIME -> {
                pdata.eventCloseTriggerCount[BasicDialogueEvents.PLAYER_ARRIVE_SOON] = 0
                pdata.eventCloseTriggerCount[BasicDialogueEvents.PLAYER_LEAVE_SOON] = 0
            }
        }

        RuinsOfGrowsseth.logDev(LogLevel.INFO, "Triggered $event ${pdata.eventTriggerCount[event]} times (close ${pdata.eventCloseTriggerCount[event] ?: 0})")
    }

    protected open fun onEventSelected(event: DialogueEvent, eventParam: String?, player: ServerPlayer, triggerSuccess: Boolean) {
        if (event == Events.PLAYER_ARRIVE_SOON) {
            playersArrivedSoon[player.uuid] = true
        } else if (event in listOf(Events.PLAYER_ARRIVE, Events.PLAYER_ARRIVE_NIGHT, Events.PLAYER_ARRIVE_LONG_TIME)) {
            playersArrivedSoon.remove(player.uuid)
        }
    }
    protected open fun onDialogueSelected(selected: DialogueEntry, event: DialogueEvent, eventParam: String?, player: ServerPlayer) {}

    override fun getDialogues(player: ServerPlayer, dialogueEvent: DialogueEvent): List<DialogueEntry> {
        return getDialoguesAndEvent(player, arrayOf(dialogueEvent))?.second ?: listOf()
    }

    protected fun getDialoguesAndEvent(
        player: ServerPlayer, dialogueEvents: Array<out DialogueEvent>, ignoreEventConditions: Boolean = false,
        ignoreEmptyWarning: Boolean = false
    ): Pair<DialogueEvent, List<DialogueEntry>>? {
        val global = DialogueEntry.getAllForEvent(Events.GLOBAL)
        for (dialogueEvent in dialogueEvents) {
            if (!ignoreEventConditions && !canTriggeredEventRun(player, dialogueEvent)) continue

            val optionsChoice = DialogueEntry.getAllForEvent(dialogueEvent)

            if (optionsChoice.isNotEmpty()) {
                return Pair(dialogueEvent, optionsChoice + global)
            }
        }
        if (!ignoreEmptyWarning && !dialogueEvents.any{it.ignoreNoDialogueWarning}) {
            RuinsOfGrowsseth.LOGGER.warn("No dialogues found or context not valid for events ${dialogueEvents.joinToString(", ") { it.toString() }}")
        }
        return null
    }

    override fun getTriggeredDialogues(player: ServerPlayer): Map<DialogueEntry, Int> {
        return playerData(player)?.dialogueCount
            ?.mapNotNull { DialogueEntry.getWithId(it.key)?.to(it.value) }
            ?.associate { it }
            ?: mapOf()
    }

    override fun getTriggeredDialogues(): Map<DialogueEntry, Int> {
//        return dialogueCount.flatMap {
//            it.value.entries.mapNotNull { entry -> DialogueEntry.getWithId(entry.key)?.to(entry.value) }
//        }
        return savedPlayersData.values.flatMap { data -> data.dialogueCount.entries.mapNotNull {
                entry -> DialogueEntry.getWithId(entry.key)?.to(entry.value)
            } }
            .groupingBy { it.first }
            .aggregate { _, accumulator: Int?, element, _ -> accumulator?.plus(element.second) ?: element.second }
    }

    private fun filterDialogueOptions(
        dialogueOptions: List<DialogueEntry>,
        player: ServerPlayer,
        event: DialogueEvent,
        eventParam: String? = null
    ): List<DialogueEntry> {
        val pdata = playerDataOrCreate(player)
        val lastForEvent = pdata.lastEventDialogue[event]
        val filters = mutableListOf<(DialogueEntry) -> Boolean>(
            { entry ->
                if (entry.useLimit != null) {
                    val entryId = entry.id
                    if (entryId == null) {
                        RuinsOfGrowsseth.LOGGER.error("Dialogue has no id but has useLimit: $entry")
                        false
                    } else {
                        val count = pdata.dialogueCount[entryId] ?: 0
                        count < entry.useLimit
                    }
                } else true
            },
            { entry ->
                entity is QuestOwner<*> && entity.quest?.let { quest ->
                    if (
                        (entry.requiresQuest != null || entry.requiresQuestStage != null || entry.requiresUntilQuestStage != null)
                    ) {
                        val goodQuest = entry.requiresQuest == null || quest.name == entry.requiresQuest
                        val goodStage = entry.requiresQuestStage == null || quest.passedStage(entry.requiresQuestStage)
                        val goodMaxStage = entry.requiresUntilQuestStage == null || !quest.passedStage(
                            entry.requiresUntilQuestStage
                        )
                        goodQuest && goodStage && goodMaxStage
                    } else true
                } ?: false
            },
        )
        pdata.eventTriggerCount[event]?.let {
            filters.add { entry -> it >= entry.afterRepeatsMin && (entry.afterRepeatsMax == null || it <= entry.afterRepeatsMax) }
        }
        pdata.eventCloseTriggerCount[event]?.let {
            filters.add { entry ->
                it >= entry.afterCloseRepeatsMin
                        && (entry.afterCloseRepeatsMax == null || it <= entry.afterCloseRepeatsMax)
            }
        }
        if (eventParam != null) {
            filters.add { entry -> entry.requiresEventParam == null || entry.requiresEventParam == eventParam }
        }
        addDialogueOptionFilters(filters, player, event, eventParam)

        val filtered = dialogueOptions.filter { entry -> filters.all { it(entry) } }
        val hasPriority = filtered.any { it.priority != 0 }
        val priorityFiltered = if (hasPriority) {
            val maxPriority = filtered.maxOf(DialogueEntry::priority)
            filtered.filter { it.priority == maxPriority }
        } else {
            val withoutLast = if (filtered.size > 1) filtered.filter { entry -> lastForEvent != entry } else filtered
            withoutLast
        }

        val postPriorityFilters = mutableListOf<(DialogueEntry) -> Boolean>()

        addDialogueOptionPostPriorityFilters(postPriorityFilters, player, event, priorityFiltered, eventParam)

        return priorityFiltered.filter { entry -> postPriorityFilters.all { it(entry) } }
    }

    /**
    * Override to add additional filters to dialogue entries. Order is:
     * - get first available event for trigger
     * - filter dialogue entries with basic filters
     * - if any of them has non-default priority, keep only ones with highest priority
     * - filter remaining entries with post priority filters
    */
    protected open fun addDialogueOptionFilters(
        filters: MutableList<(DialogueEntry) -> Boolean>, player: ServerPlayer, event: DialogueEvent, eventParam: String? = null
    ) {}

    /**
     * Override to add filters to dialogue entries that are applied after priority is checked (and so, after the first filters are applied).
     * The order is:
     * - get first available event for trigger
     * - filter dialogue entries with basic filters
     * - if any of them has non-default priority, keep only ones with highest priority
     * - filter remaining entries with post priority filters
     */
    protected open fun addDialogueOptionPostPriorityFilters(
        postPriorityFilters: MutableList<(DialogueEntry) -> Boolean>,
        player: ServerPlayer, event: DialogueEvent, currentEntries: List<DialogueEntry>,
        eventParam: String? = null,
    ) {}

    private fun getPlayerById(uuid: UUID): ServerPlayer? {
        val player = entity.level().getPlayerByUUID(uuid)
        return if (player != null) {
            player as ServerPlayer
        } else {
            playerDataOrCreate(uuid).lastSeenTimestamp = entity.level().gameTime
            closePlayers.remove(uuid)

            null
        }
    }

    override fun nearbyPlayers(): List<ServerPlayer> = closePlayers.toList().mapNotNull { getPlayerById(it) }

    private fun eventInQueue(event: DialogueEvent): Boolean {
        return dialogueQueues.any { e -> e.value.any { it.second == event } }
    }

    protected fun getSecondsSinceTick(entity: Entity, tick: Int): Double {
        return (entity.tickCount - tick) / 20.0
    }

    protected fun getSecondsSinceWorldTime(time: Long): Double {
        return (entity.level().gameTime - time) / 20.0
    }

    protected fun estimateReadingTime(text: String, wordsPerMinute: Int = MiscConfig.dialogueWordsPerMinute): Float {
        // Calculate the number of words in the text, ignore small words (<=2 chars)
        val wordCount = text.split(Regex("\\s+")).filter { it.replace(Regex("[^A-Za-z0-9\\\\s]"), "").length > 2 }.size

        // Calculate the reading time in minutes
        val readingTimeInMinutes = wordCount / wordsPerMinute.toFloat()

        // Return the reading time in seconds
        return readingTimeInMinutes * 60
    }

    protected open fun addExtraNbtData(dialogueData: CompoundTag) {}
    protected open fun readExtraNbtData(dialogueData: CompoundTag) {}
    protected open val saveNbtPersistData: Boolean = false

    override fun writeNbt(tag: CompoundTag) {
        val data = CompoundTag()
        tag.put("DialogueData", data)

        data.saveField(DataFields.CLOSE_PLAYERS, UUIDUtil.STRING_CODEC.mutableSetOf(), ::closePlayers)
        data.saveField(DataFields.LEAVING_PLAYERS, mutableMapCodec(UUIDUtil.STRING_CODEC, Codec.INT), ::leavingPlayers)
        data.saveField(DataFields.PLAYERS_ARRIVED_SOON, mutableMapCodec(UUIDUtil.STRING_CODEC, Codec.BOOL), ::playersArrivedSoon)

        if (saveNbtPersistData)
            data.saveField(DataFields.SAVED_PLAYERS_DATA, mutableMapCodec(UUIDUtil.STRING_CODEC, PLAYER_DATA_CODEC), ::savedPlayersData)

        addExtraNbtData(data)
    }

    override fun readNbt(tag: CompoundTag) {
        closePlayers.clear()
        leavingPlayers.clear()
        closePlayers.clear()
        savedPlayersData.clear()
        tag.get("DialogueData")?.let { data -> if (data is CompoundTag) {
            data.loadField(DataFields.CLOSE_PLAYERS, UUIDUtil.STRING_CODEC.mutableSetOf()) { closePlayers.addAll(it) }
            data.loadField(DataFields.LEAVING_PLAYERS, mutableMapCodec(UUIDUtil.STRING_CODEC, Codec.INT)) { leavingPlayers.putAll(it) }
            data.loadField(DataFields.PLAYERS_ARRIVED_SOON, mutableMapCodec(UUIDUtil.STRING_CODEC, Codec.BOOL)) { playersArrivedSoon.putAll(it) }

            if (saveNbtPersistData)
                data.loadField(DataFields.SAVED_PLAYERS_DATA, mutableMapCodec(UUIDUtil.STRING_CODEC, PLAYER_DATA_CODEC)) { savedPlayersData.putAll(it) }

            readExtraNbtData(data)
        }}
    }

    object DataFields {
        const val CLOSE_PLAYERS = "closePlayers"
        const val LEAVING_PLAYERS = "leavingPlayers"
        const val PLAYERS_ARRIVED_SOON = "playersArrivedSoon"
        const val SAVED_PLAYERS_DATA = "savedPlayersData"
    }

    object Callbacks {
        /*fun onAttack(player: Player, world: Level, hand: InteractionHand, entity: Entity, hitResult: EntityHitResult?): InteractionResult {
            if (!world.isClientSide && entity is DialoguesNpc && !player.isCreative) {
                val dialogues = entity.dialogues
                if (dialogues is BasicDialoguesComponent) {
                    //dialogues.playersWhoAttackedRecently[player.uuid] = true
                    dialogues.triggerDialogue(player as ServerPlayer, Events.HIT_BY_PLAYER)
                }
            }
            return InteractionResult.PASS
        }*/

        fun onAdvancement(player: ServerPlayer, advancement: AdvancementHolder, criterionKey: String) {
            // Rather than search dialogue npcs on every event, mark the player for checking stuff
            // later (better for performance?)
            val level = player.serverLevel()
            val searchRadius = 80.0
            val dialoguesNpcs = level.getDialogueNpcs(AABB.ofSize(player.position(), searchRadius, searchRadius, searchRadius))
            dialoguesNpcs.forEach {
                val dialogues = it.dialogues
                if (dialogues is BasicDialoguesComponent && dialogues.nearbyPlayers().contains(player)) {
                    dialogues.triggerDialogue(player, BasicDialogueEvents.PLAYER_ADVANCEMENT, eventParam = advancement.id.toString())
                }
            }
        }
    }
}