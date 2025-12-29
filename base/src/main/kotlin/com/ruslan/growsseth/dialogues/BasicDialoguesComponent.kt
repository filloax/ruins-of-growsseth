package com.ruslan.growsseth.dialogues

import com.filloax.fxlib.api.codec.mapWithValueOf
import com.filloax.fxlib.api.codec.mutableMapCodec
import com.filloax.fxlib.api.codec.mutableSetOf
import com.filloax.fxlib.api.nbt.loadField
import com.filloax.fxlib.api.nbt.saveField
import com.filloax.fxlib.api.networking.sendPacket
import com.filloax.fxlib.api.optional
import com.filloax.fxlib.api.secondsToTicks
import com.filloax.fxlib.api.weightedRandom
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.MiscConfig
import com.ruslan.growsseth.dialogues.BasicDialogueEvents
import com.ruslan.growsseth.dialogues.DialoguesNpc.Companion.getDialogueNpcs
import com.ruslan.growsseth.network.DialoguePacket
import com.ruslan.growsseth.network.DialogueSeparatorPacket
import com.ruslan.growsseth.quests.QuestOwner
import com.ruslan.growsseth.utils.notNull
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
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
            Codec.STRING.mapWithValueOf(Codec.INT).optionalFieldOf("dialogueGroupCount", mutableMapOf()).forGetter(PlayerData::dialogueGroupCount),
            DialogueEvent.CODEC.mapWithValueOf(Codec.INT).fieldOf("eventTriggerCount").forGetter(PlayerData::eventTriggerCount),
            DialogueEvent.CODEC.mapWithValueOf(Codec.INT).fieldOf("eventCloseTriggerCount").forGetter(PlayerData::eventCloseTriggerCount),
            DialogueEvent.CODEC.mapWithValueOf(Codec.LONG).fieldOf("eventLastTriggerTime").forGetter(PlayerData::eventLastTriggerTime),
            Codec.LONG.optionalFieldOf("lastSeenTimestamp").forGetter(PlayerData::lastSeenTimestamp.optional()),
            Codec.LONG.optionalFieldOf("lastArrivedTimestamp").forGetter(PlayerData::lastArrivedTimestamp.optional()),
        ).apply(builder) { d, g, et, ec, el, ls, la ->  PlayerData(d, g, et, ec, el, ls.getOrNull(), la.getOrNull()) } }

        val TARGETING: TargetingConditions = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting()
    }

    open var nearbyRadius = 12.0
    open var radiusForTriggerLeave = 17.0
    open var secondsForTriggerLeave = 0   // waiting time before saying goodbye when a player leaves
    open var checkLineOfSight = true
    /** Set to 0 to disable "soon" events: */
    open var secondsForArriveSoon = 10
    /** Set to 0 to disable "long time" events: */
    open var secondsForArriveLongTime = 6 * 3600 // 6 hours
    open var secondsForCloseRepeat = 60
    open var secondsForAttackDiagRepeat = 10
    open var maxCloseHitsForDialogues = 3
    /** Set to 0 to have no wait: */
    open var dialogueDelayMaxSeconds = 0.6f

    // NBT data
    // First key is player UUID
    protected val closePlayers = mutableSetOf<UUID>()
    protected val leavingPlayers = mutableMapOf<UUID, Int>()
    protected val playersArrivedSoon = mutableMapOf<UUID, Boolean>()
    protected val savedPlayersData = mutableMapOf<UUID, PlayerData>()
    protected var numOfInbetweenPlayers: Int = 0

    // UUID is player's
    // Each Deque is the queue of events to trigger, which contains after they are resolved
    // (lazily, when the previous dialogue is done) the list of dialogue lines that will play
    // for that instance of the event
    protected val eventQueues = mutableMapOf<UUID, Deque<EventQueueItem>>()
    protected var dialogueQueueDelays = mutableMapOf<UUID, Int>()
    private   val playersLastSentSeparator = mutableSetOf<UUID>()
    protected val serverLevel: ServerLevel get() = entity.level() as ServerLevel
    protected val server get() = serverLevel.server

    protected fun playerDataOrCreate(player: ServerPlayer) = playerDataOrCreate(player.uuid)
    protected fun playerDataOrCreate(uuid: UUID) = savedPlayersData.computeIfAbsent(uuid) { PlayerData() }
    protected fun playerData(player: ServerPlayer) = playerData(player.uuid)
    protected fun playerData(uuid: UUID) = savedPlayersData[uuid]


    protected open fun onPlayerArrive(player: ServerPlayer) {
        triggerDialogue(player, Events.PLAYER_ARRIVE_LONG_TIME, Events.PLAYER_ARRIVE_SOON, Events.PLAYER_ARRIVE_NIGHT, Events.PLAYER_ARRIVE)
    }

    protected open fun onPlayerLeave(player: ServerPlayer) {
        playerDataOrCreate(player).lastSeenTimestamp = entity.level().gameTime
        // just make a blank one if event queue not init, no need to modify original
        val eventQueue = eventQueues[player.uuid] ?: LinkedBlockingDeque()

        // Handle interruptions
        // TODO: save current dialogue status to resume
        val interrupted = eventQueue.any { event ->     // got interrupted if there is at least one valid dialogue in queue
             resolveDialogueEventQueueItem(player, event, checkOnly = true)
        }
        eventQueue.clear()

        if (!player.isDeadOrDying) { // to avoid goodbye when the npc gets away from the place where player died (and did not respawn yet)
            if (interrupted) {
                triggerDialogue(player, Events.PLAYER_LEAVE_INTERRUPTED)
            } else {
                triggerDialogue(player, Events.PLAYER_LEAVE_SOON, Events.PLAYER_LEAVE_NIGHT, Events.PLAYER_LEAVE)
            }
        }
    }

    protected open fun onPlayerTickNear(player: ServerPlayer) {
        triggerDialogueInternal(player, Events.TICK_NEAR_PLAYER, ignoreEmptyOptionsWarning = true, countEvents = false, eagerResolve = true)
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
            Events.PLAYER_ARRIVE_NIGHT -> entity.level().isDarkOutside
            Events.PLAYER_ARRIVE_SOON -> secondsForArriveSoon > 0
                    && playerData(player)?.lastSeenTimestamp?.let { getSecondsSinceWorldTime(it) < secondsForArriveSoon } == true
            Events.PLAYER_ARRIVE_LONG_TIME ->  secondsForArriveLongTime > 0
                    && playerData(player)?.lastSeenTimestamp?.let { getSecondsSinceWorldTime(it) > secondsForArriveLongTime } == true
            Events.PLAYER_LEAVE_NIGHT -> entity.level().isDarkOutside
            Events.PLAYER_LEAVE_SOON -> secondsForArriveSoon > 0
                    && playersArrivedSoon[player.uuid] ?: false
            Events.HIT_BY_PLAYER -> secondsForAttackDiagRepeat > 0
            Events.RENAME -> !eventInQueue(dialogueEvent)
            else -> true
        }
    }

    override fun skipCurrentMessage(player: ServerPlayer): Boolean {
        val playerEventQueue = eventQueues.getOrDefault(player.uuid, null)
        if (playerEventQueue.isNullOrEmpty())
            return false

        // We search every event queue of the player for valid events with dialogues, and reset the current delay at the first one we find
        playerEventQueue.forEach { eventQueueItem ->
            val validDialoguesInQueueItem = resolveDialogueEventQueueItem(player, eventQueueItem, checkOnly = true)
            if (validDialoguesInQueueItem) {
                dialogueQueueDelays[player.uuid] = 0
                return true
            }
        }

        return false        // no actual dialogues have been found, there is no skip to do
    }

    override fun dialoguesStep() {
        // Run every 2 ticks for better performance
        if (entity.tickCount % 2 == 0) {
            checkNearbyPlayers()
        }

        for ((playerUuid, eventQueue) in eventQueues) {
            var dialogueQueueDelay = dialogueQueueDelays.computeIfAbsent(playerUuid) { 0 }

            val player = server.playerList.getPlayer(playerUuid)
            if (player == null) {
                RuinsOfGrowsseth.LOGGER.warn("Player $playerUuid left game while dialogues were still queued!")
                eventQueue.clear()
                continue
            }
            if (dialogueQueueDelay > 0) {
                dialogueQueueDelay--
            }
            if (eventQueue.isNotEmpty() && dialogueQueueDelay <= 0) {
                popQueues(player, eventQueue)?.let { nextDialogueDelay ->
                    dialogueQueueDelay = nextDialogueDelay
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
            && it.isAlive
        }.toMutableSet()

        // Players far enough to trigger leave
        val farPlayers = possiblePlayers.filterNot { farBoundingBox.contains(it.position()) }.toMutableSet()
        changeNearPlayers(nearPlayers, farPlayers)

        // Player not in either area
        val inbetweenPlayers = possiblePlayers - nearPlayers - farPlayers
        numOfInbetweenPlayers = inbetweenPlayers.size

        for (player in nearPlayers) {
            if (player.uuid !in closePlayers && !player.isSpectator) {
                closePlayers.add(player.uuid)
                onPlayerArrive(player)
            }
            if (player.uuid in closePlayers && player.isSpectator) {
                closePlayers.remove(player.uuid)
            }
            if (player.uuid in leavingPlayers) {
                leavingPlayers.remove(player.uuid)
            }
            onPlayerTickNear(player)
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

    override fun sendDialogueToPlayer(player: ServerPlayer, line: DialogueLineProcessed) {
        if (line.text.isNotEmpty()) {
            playersLastSentSeparator.remove(player.uuid)
            player.sendPacket(DialoguePacket(line, entity.name, entity.uuid))
        }
    }

    protected open fun sendSeparatorToPlayer(player: ServerPlayer) {
        player.sendPacket(DialogueSeparatorPacket(entity.name, entity.uuid))
        playersLastSentSeparator.add(player.uuid)
    }

    override fun triggerDialogueEntry(player: ServerPlayer, dialogueEntry: DialogueEntry, immediate: Boolean) {
        // Build ad-hoc event queue item
        val event = BasicDialogueEvents.MANUAL_TRIGGER
        val queueItem = EventQueueItem(listOf(event), null, true)
        val lines = dialogueEntry.content.map { materializeDialogueLine(dialogueEntry, it) }
        queueItem.resolve(event, LinkedBlockingDeque(lines))

        val eventQueue = eventQueues.computeIfAbsent(player.uuid) { LinkedBlockingDeque() }
        offerEventQueueItem(player, eventQueue, queueItem, immediate)
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
    ) {
        if (player.isSpectator)
            return
        triggerDialogueInternal(player, *dialogueEvents, eventParam=eventParam, ignoreEventConditions=ignoreEventConditions)
    }

    /**
     * Implementation of [triggerDialogue] with extra options to
     * allow more control internally in this class
     * @param countEvents some events are meant to be called often, and leave
     *   selection of when to play dialogues in the specific dialogue conditions.
     * @param eagerResolve if this trigger is called often (every tick, etc.)
     *  eager check if it has valid dialogue to play
     *  BEFORE queueing to avoid infinite queues from the "spammed"
     *  event. Will also recheck before starting the dialogue to make sure the conditions
     *  are still relevant.
     */
    private fun triggerDialogueInternal(
        player: ServerPlayer,
        vararg dialogueEvents: DialogueEvent,
        eventParam: String? = null,
        ignoreEventConditions: Boolean = false,
        ignoreEmptyOptionsWarning: Boolean = false,
        countEvents: Boolean = true,
        eagerResolve: Boolean = false,
    ) {
        // Resolve dialogue selection, filtering based on conditions, etc.
        // lazily: as the completion of a dialogue affects the conditions of
        // following dialogues (dialogues requiring a count, etc.), we decide
        // which specific dialogue event and entry to use when that event's
        // turn comes (that is done inside `resolveDialogueEventQueueItem`)

        val eventQueue = eventQueues.computeIfAbsent(player.uuid) { LinkedBlockingDeque() }

        val queueItem = EventQueueItem(dialogueEvents.toList(), eventParam, ignoreEventConditions, ignoreEmptyOptionsWarning, countEvents)

        val anyImmediate = dialogueEvents.any { it.immediate }
        if (anyImmediate && dialogueEvents.any { !it.immediate }) {
            throw IllegalArgumentException("Cannot use mix of immediate and not immediate dialogue events! Was ${dialogueEvents.joinToString { it.id }}")
        }
        val anyPreventMultiQueue = dialogueEvents.any { it.preventMultiQueue }
        if (anyPreventMultiQueue && dialogueEvents.any { !it.preventMultiQueue }) {
            throw IllegalArgumentException("Cannot use mix of preventMultiQueue and not preventMultiQueue dialogue events! Was ${dialogueEvents.joinToString { it.id }}")
        }
        
        if (anyPreventMultiQueue && dialogueEvents.any(::eventInQueue)) {
            // Already queued and should not requeue
            return
        }

        val runImmediately = dialogueDelayMaxSeconds == 0f || anyImmediate

        if (eventQueue.isEmpty() || runImmediately || eagerResolve) {
            if (!eagerResolve) {
                RuinsOfGrowsseth.LOGGER.debug("Resolving dialogue event immediately: {} for {}", queueItem, player)
            }

            val checkOnly = eagerResolve && !(eventQueue.isEmpty() || runImmediately)

            if (!resolveDialogueEventQueueItem(player, queueItem, checkOnly)) {
                // no dialogues found, do not queue
                return
            }
        }

        offerEventQueueItem(player, eventQueue, queueItem, anyImmediate)
    }

    private fun offerEventQueueItem(player: ServerPlayer, eventQueue: Deque<EventQueueItem>, queueItem: EventQueueItem, immediate: Boolean) {
        // If run immediately: place at beginning of queue and set delay
        // for next dialogue to 0 (so it plays on next tick), avoid
        // directly playing it here to not repeat logic of dialogue completion, etc.
        if (immediate) {
            // If interrupting something, send separator unless it was the last thing sent
            if (eventQueue.isNotEmpty() && !playersLastSentSeparator.contains(player.uuid)) {
                sendSeparatorToPlayer(player)
            }

            RuinsOfGrowsseth.LOGGER.debug("Queueing dialogue event immediately: {} for {}", queueItem, player)
            eventQueue.offerFirst(queueItem)
            dialogueQueueDelays[player.uuid] = 0
        } else {
            RuinsOfGrowsseth.LOGGER.debug("Queueing dialogue event: {} for {}", queueItem, player)
            eventQueue.offer(queueItem)
        }
    }

    /**
     * @param eventQueue must not be empty
     * @return delay for next dialogue if any dialogue send action taken, null otherwise
     */
    private fun popQueues(player: ServerPlayer, eventQueue: Deque<EventQueueItem>): Int? {
        if (eventQueue.isEmpty()) return null

        var eventQueueItem: EventQueueItem = eventQueue.peek()
        while (eventQueue.isNotEmpty() && !eventQueueItem.resolved) {
            if (!resolveDialogueEventQueueItem(player, eventQueueItem)) {
                eventQueueItem = eventQueue.pop()
            }
        }
        if (eventQueueItem.failed) {
            if (eventQueue.isNotEmpty()) {
                // Last item checked is failed, pop and return false
                eventQueue.pop()
            }
            return null
        }
        if (!eventQueueItem.resolved) {
            // should not happen
            RuinsOfGrowsseth.LOGGER.warn("Current dialogue queue item not resolved after pop checks!")
            return null
        }

        val dialogueQueue = eventQueueItem.dialogueQueue

        if (dialogueQueue.isEmpty()) {
            RuinsOfGrowsseth.LOGGER.warn("Tried popping empty dialogue queue, cancel")
            // Should not happen as event queue is popped when current dialogue empty, but fallback
            eventQueue.pop()
            return null
        }

        val line = dialogueQueue.pop()
        // Last line of dialogue entry
        val completed = dialogueQueue.isEmpty()

        sendDialogueToPlayer(player, line)

        if (completed) {
            if (line.text.isNotEmpty()) {
                sendSeparatorToPlayer(player)
            }

            onDialogueComplete(player, line.dialogue, eventQueueItem.event, eventQueueItem.eventParam, eventQueueItem.countEvents)

            if (eventQueue.isNotEmpty()) {
                eventQueue.pop() // remove current item to make space for next
            }
        }

        return if (MiscConfig.dialogueWordsPerMinute > 0)
                line.duration.secondsToTicks()
            else 0
    }

    /**
     * Do the actual dialogue entry condition filtering
     * choosing which event from a list of events has matching conditions and has entries
     * and which of its dialogue entries to use depending on priority/conditions.
     *
     * Alters and returns the queueItem, setting [EventQueueItem.resolved] to true and filling
     * the event and dialogueEvent fields on a success, or [EventQueueItem.failed] to true on
     * no dialogues found.
     *
     * @param checkOnly Only check if there are any valid dialogues (returning true/false), but do not alter
     *   the passed queueItem.
     * @return true if found any dialogue
     */
    private fun resolveDialogueEventQueueItem(player: ServerPlayer, queueItem: EventQueueItem, checkOnly: Boolean = false): Boolean {
        if (queueItem.resolved) {
            if (!checkOnly)
                RuinsOfGrowsseth.LOGGER.warn("Tried resolving already resolved dialogue event queue item {}", queueItem)
            return true
        } else if (queueItem.failed) {
            if (!checkOnly)
                RuinsOfGrowsseth.LOGGER.warn("Tried resolving failed dialogue event queue item {}", queueItem)
            return false
        }

        val (dialogueEvents, eventParam, ignoreEventConditions, ignoreEmptyOptionsWarning) = queueItem

        val (event, dialogueOptions) = getDialoguesAndEvent(player, dialogueEvents, ignoreEventConditions, ignoreEmptyOptionsWarning)
            ?: run {
                if (!checkOnly)
                    queueItem.fail()
                return false
            }

        val validOptions = filterDialogueOptions(dialogueOptions, player, event, eventParam)
        val success = validOptions.isNotEmpty()

        if (!checkOnly) {
            onEventSelected(event, eventParam, player, success)
        }

        if (!success) {
//            if (!ignoreEmptyOptionsWarning && !dialogueEvents.any{it.ignoreNoDialogueWarning}) {
//                RuinsOfGrowsseth.LOGGER.warn("No valid dialogue options for $event (param=$eventParam) $entity")
//            }
            if (!checkOnly) {
                queueItem.fail()
            }
            return false
        } else if (checkOnly) {
            return true
        }

        val selected = validOptions.weightedRandom(DialogueEntry::weight::get, random)

        onDialogueSelected(player, selected, event, eventParam)

        val dialogueQueue: Deque<DialogueLineProcessed> = selected.content
            .map { materializeDialogueLine(selected, it) }
            .let { LinkedBlockingDeque(it) }

        queueItem.resolve(event, dialogueQueue)

        return true
    }

    private fun filterDialogueOptions(
        dialogueOptions: List<DialogueEntry>,
        player: ServerPlayer,
        event: DialogueEvent,
        eventParam: String? = null
    ): List<DialogueEntry> {
        val pdata = playerDataOrCreate(player)
        val lastForEvent = pdata.lastEventDialogue[event]

        // +1 to count current trigger
        val eventTriggerCount = (pdata.eventTriggerCount[event] ?: 0) + 1
        val eventCloseTriggerCount = (pdata.eventCloseTriggerCount[event] ?: 0) + 1

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
                if (entry.groupUseLimit != null) {
                    if (entry.groups == null) {
                        RuinsOfGrowsseth.LOGGER.error("Dialogue has no groups but has groupUseLimit: $entry")
                        false
                    } else {
                        entry.groups.all { group ->
                            val count = pdata.dialogueGroupCount[group] ?: 0
                            count < entry.groupUseLimit
                        }
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
            { entry ->
                eventTriggerCount >= entry.afterRepeatsMin && (entry.afterRepeatsMax == null || eventTriggerCount <= entry.afterRepeatsMax)
            },
            { entry ->
                eventCloseTriggerCount >= entry.afterCloseRepeatsMin
                && (entry.afterCloseRepeatsMax == null || eventCloseTriggerCount <= entry.afterCloseRepeatsMax)
            },
        )

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

    protected open fun onDialogueSelected(
        player: ServerPlayer,
        dialogueEntry: DialogueEntry,
        event: DialogueEvent,
        eventParam: String?,
    ) {
    }

    protected open fun onDialogueComplete(
        player: ServerPlayer,
        dialogueEntry: DialogueEntry,
        event: DialogueEvent,
        eventParam: String?,
        countEvents: Boolean = true,
    ) {
        RuinsOfGrowsseth.LOGGER.debug("Completed dialogue {}, event {}, for player {}", dialogueEntry, event, player)

        val playerData = playerDataOrCreate(player)
        playerData.lastEventDialogue[event] = dialogueEntry

        if (countEvents && event.count) {
            incrementEventCount(event, player)
        }

        if (dialogueEntry.id != null) {
            playerData.dialogueCount.let{ map -> map[dialogueEntry.id] = map.getOrDefault(dialogueEntry.id, 0) + 1 }
        }
        dialogueEntry.groups?.forEach { group ->
            playerData.dialogueGroupCount.let{ map -> map[group] = map.getOrDefault(group, 0) + 1 }
        }
    }

    protected open fun incrementEventCount(event: DialogueEvent, player: ServerPlayer) {
        //to avoid repetitions
        val time =  entity.level().gameTime
        val pdata = playerDataOrCreate(player)
        val lastTriggerTime = pdata.eventLastTriggerTime[event]

        if (event == Events.HIT_BY_PLAYER && lastTriggerTime != null) {
            val count = pdata.eventCloseTriggerCount[event] ?: 1
            val secondsSinceLastAttack = getSecondsSinceWorldTime(lastTriggerTime)
            if (secondsSinceLastAttack < secondsForAttackDiagRepeat && count < maxCloseHitsForDialogues)
                pdata.eventCloseTriggerCount[event] = count + 1
            else if (secondsSinceLastAttack > secondsForAttackDiagRepeat * 2)
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

    override fun getDialogues(player: ServerPlayer, dialogueEvent: DialogueEvent): List<DialogueEntry> {
        return getDialoguesAndEvent(player, listOf(dialogueEvent))?.second ?: listOf()
    }

    private fun getDialoguesAndEvent(
        player: ServerPlayer, dialogueEvents: Collection<DialogueEvent>,
        ignoreEventConditions: Boolean = false, ignoreEmptyWarning: Boolean = false
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

    override fun getTriggeredDialogueGroups(player: ServerPlayer): Map<String, Int> {
        return playerData(player)?.dialogueGroupCount ?: mapOf()
    }

    override fun getTriggeredDialogueGroups(): Map<String, Int> {
        return savedPlayersData.values.flatMap { data -> data.dialogueGroupCount.entries }
            .groupingBy { it.key }
            .aggregate { _, accumulator: Int?, element, _ -> accumulator?.plus(element.value) ?: element.value }
    }

    private fun getPlayerById(uuid: UUID): ServerPlayer? {
        val player = entity.level().getPlayerByUUID(uuid)
        return if (notNull(player)) {
            player as ServerPlayer
        } else {
            playerDataOrCreate(uuid).lastSeenTimestamp = entity.level().gameTime
            closePlayers.remove(uuid)

            null
        }
    }

    override fun nearbyPlayers(): List<ServerPlayer> = closePlayers.toList().mapNotNull { getPlayerById(it) }
    // needed to avoid npcs getting away from players before saying goodbye
    override fun playersStillAround(): Boolean = closePlayers.size + numOfInbetweenPlayers > 0

    /**
     * Returns true if event may be in queue (not 100% sure as multi-event queued items
     * could have that event or not)
     */
    private fun eventInQueue(event: DialogueEvent): Boolean {
        return eventQueues.values.any { e -> e.any {
            if (it.resolved) {
                it.event == event
            } else {
                it.dialogueEvents.contains(event)
            }
        } }
    }

    private fun getSecondsSinceTick(entity: Entity, tick: Int): Double {
        return (entity.tickCount - tick) / 20.0
    }

    private fun getSecondsSinceWorldTime(time: Long): Double {
        return (entity.level().gameTime - time) / 20.0
    }

    private fun materializeDialogueLine(entry: DialogueEntry, line: DialogueLine): DialogueLineProcessed {
        return DialogueLineProcessed(line.content(), estimateReadingTime(line)).also {
            it.dialogue = entry
        }
    }

    private fun estimateReadingTime(line: DialogueLine, wordsPerMinute: Int = MiscConfig.dialogueWordsPerMinute): Float {
        val text = line.content()

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

    data class PlayerData(
        val dialogueCount: MutableMap<String, Int> = mutableMapOf(),
        val dialogueGroupCount: MutableMap<String, Int> = mutableMapOf(),
        val eventTriggerCount: MutableMap<DialogueEvent, Int> = mutableMapOf(),
        val eventCloseTriggerCount: MutableMap<DialogueEvent, Int> = mutableMapOf(),
        val eventLastTriggerTime: MutableMap<DialogueEvent, Long> = mutableMapOf(),
        var lastSeenTimestamp: Long? = null,
        var lastArrivedTimestamp: Long? = null,
        // Do not persist, not necessary
        val lastEventDialogue: MutableMap<DialogueEvent, DialogueEntry> = mutableMapOf(),
    )

    data class EventQueueItem(
        val dialogueEvents: List<DialogueEvent>,
        val eventParam: String? = null,
        val ignoreEventConditions: Boolean = false,
        val ignoreEmptyOptionsWarning: Boolean = false,
        val countEvents: Boolean = true,
        private var failed_: Boolean = false,
        private var resolved_: Boolean = false,

        // These two should be non-null IF AND ONLY IF
        // resolved is true (would make other class, but logic is simpler
        // if I just expand the same object in the queue with dialogues
        private var event_: DialogueEvent? = null,
        private var dialogueQueue_: Deque<DialogueLineProcessed>? = null,
    ) {
        // Assign a specific event & dialogue queue
        fun resolve(event: DialogueEvent, dialogueQueue: Deque<DialogueLineProcessed>): EventQueueItem {
            if (resolved_) {
                throw IllegalStateException("Dialogue event item already resolved: $this")
            }
            resolved_ = true
            event_ = event
            dialogueQueue_ = dialogueQueue

            return this
        }

        fun fail(): EventQueueItem {
            failed_ = true

            return this
        }

        val failed get() = failed_
        val resolved get() = resolved_
        val event get() = event_ ?: throw IllegalStateException("Tried obtaining event on not resolved queue item $this")
        val dialogueQueue get() = dialogueQueue_ ?: throw IllegalStateException("Tried obtaining dialogueQueue on not resolved queue item $this")
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
            // Rather than search dialogue npcs on every event, mark the player for checking stuff later (better for performance?)
            val level = player.level()
            val searchRadius = 80.0
            val dialoguesNpcs = level.getDialogueNpcs(AABB.ofSize(player.position(), searchRadius, searchRadius, searchRadius))
            dialoguesNpcs.forEach {
                val dialogues = it.dialogues
                if (dialogues is BasicDialoguesComponent && dialogues.nearbyPlayers().contains(player)) {
                    // Trigger both, dialogue conditions will take care of deciding which one to play
                    dialogues.triggerDialogue(player, BasicDialogueEvents.PLAYER_ADVANCEMENT, eventParam = advancement.id.toString())
                    dialogues.triggerDialogue(player, BasicDialogueEvents.PLAYER_ADVANCEMENT_LAZY, eventParam = advancement.id.toString())
                }
            }
        }
    }
}