package com.ruslan.growsseth.dialogues

import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ResearcherDialoguesComponent
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB

/**
 * Interface to represent a component that handles "dialogues" for an NPC,
 * with various triggers
 */
interface NpcDialoguesComponent {
    /**
     * Send a dialogue to a player, as a chat message,
     * some sort of ui popup, etc.
     */
    fun sendDialogueToPlayer(player: ServerPlayer, line: DialogueLine)
    fun dialoguesStep()
    fun nearbyPlayers(): List<ServerPlayer>

    /**
     * Reset nearby players, use to clear the list and allow new greetings
     * without triggering goodbyes
     */
    fun resetNearbyPlayers()

    fun getDialogues(player: ServerPlayer, dialogueEvent: DialogueEvent): List<DialogueEntry>

    /**
     * Get a list of all the dialogues that were triggered by this NPC, and the amount of times they were triggered.
     * Usually only dialogues with an id can be remembered this way.
     */
    fun getTriggeredDialogues(player: ServerPlayer): Map<DialogueEntry, Int>

    /**
     * Get a list of all the dialogues that were triggered by this NPC for all players,
     * and the amount of times they were triggered.
     * Usually only dialogues with an id can be remembered this way.
     */
    fun getTriggeredDialogues(): Map<DialogueEntry, Int>

    /**
     * Can the event run for this player in the current conditions and context?
     * This is supposed to be used when the trigger conditions for the event are already met;
     * for example, on PLAYER_ARRIVE, a player comes close, then this checks if the event
     * can run in that instance.
     */
    fun canTriggeredEventRun(player: ServerPlayer, dialogueEvent: DialogueEvent): Boolean


    /**
     * Runs a random dialogue, using [canTriggeredEventRun] to check if it can run.
     * (devs: make it respect this contract)
     * @param dialogueEvents Dialogue events to triggger, in order of priority
     *  (will use next ones if previous are empty)
     * @param eventParam Parameter for the event to use in filtering dialogues
     * @param ignoreEventConditions if true: do not run checks with [canTriggeredEventRun]
     * @return If a dialogue was triggered
     */
    fun triggerDialogue(player: ServerPlayer, vararg dialogueEvents: DialogueEvent, eventParam: String? = null, ignoreEventConditions: Boolean = false): Boolean

    /**
     * Triggers a dialogue entry, otherwise respecting its params (timing, immediate, etc)
     */
    fun triggerDialogueEntry(player: ServerPlayer, dialogueEntry: DialogueEntry)

    fun readNbt(tag: CompoundTag) {}
    fun writeNbt(tag: CompoundTag) {}
}

interface DialoguesNpc {
    /**
     * Should be null on client side, and never null on server side
     */
    val dialogues: NpcDialoguesComponent?

    companion object {
        fun ServerLevel.getDialogueNpcs(): List<DialoguesNpc> {
            return this.getEntities(EntityTypeTest.forClass(LivingEntity::class.java)) {
                it is DialoguesNpc
            }.map { it as DialoguesNpc }
        }

        fun ServerLevel.getDialogueNpcs(area: AABB): List<DialoguesNpc> {
            return this.getEntities(EntityTypeTest.forClass(LivingEntity::class.java), area) {
                it is DialoguesNpc
            }.map { it as DialoguesNpc }
        }
    }
}