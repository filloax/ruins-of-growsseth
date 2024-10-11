package com.ruslan.growsseth.dialogues

import com.mojang.serialization.Codec
import java.lang.IllegalStateException
import com.ruslan.growsseth.dialogues.DialogueEvent.Companion.event

object BasicDialogueEvents {
    /** Triggers on all events (use filters and priority) */
    val GLOBAL = event("global")
    val PLAYER_ARRIVE = event("playerArrive", tag=DialogueEvent.TAG_HELLO)
    val PLAYER_ARRIVE_NIGHT = event("playerArriveNight", tag=DialogueEvent.TAG_HELLO)
    val PLAYER_LEAVE = event("playerLeave", tag=DialogueEvent.TAG_GOODBYE)
    val PLAYER_LEAVE_NIGHT = event("playerLeaveNight", tag=DialogueEvent.TAG_GOODBYE)
    val PLAYER_ARRIVE_SOON = event("playerArriveSoon", tag=DialogueEvent.TAG_HELLO)
    val PLAYER_LEAVE_SOON = event("playerLeaveSoon", tag=DialogueEvent.TAG_GOODBYE)
    val PLAYER_ARRIVE_LONG_TIME = event("playerArriveLongTime", tag=DialogueEvent.TAG_HELLO)
    /** As soon as the dialogue conditions apply and a player is nearby, this triggers;
     * to be used by limited dialogue entries. */
    val TICK_NEAR_PLAYER = event("tickNearPlayer")
    val HIT_BY_PLAYER = event("hitByPlayer")
    val LOW_HEALTH = event("lowHealth")
    val DEATH = event("death")
    /** To be triggered on rename, not handled automatically */
    val RENAME = event("rename")
    val PLAYER_ADVANCEMENT = event("playerAdvancement")
    val MANUAL_TRIGGER = event("manualTrigger")
}

class DialogueEvent private constructor (
    val id: String,
    val ignoreNoDialogueWarning: Boolean = false,
    val count: Boolean = true,
    val tags: Set<String> = setOf(),
) {
    override fun toString(): String {
        return "<$id>"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DialogueEvent

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        val CODEC: Codec<DialogueEvent> = Codec.STRING.xmap({ getById(it) }, DialogueEvent::id)

        const val TAG_HELLO = "hello"
        const val TAG_GOODBYE = "goodbye"


        fun event(
            id: String,
            tags: Set<String> = setOf(),
            tag: String? = null,
            ignoreNoDialogueWarning: Boolean = false,
            count: Boolean = true,
        ): DialogueEvent {
            var tagsHolder = tags;
            if (tag != null) {
                tagsHolder = tagsHolder.plus(tag)
            }
            val event = DialogueEvent(id, ignoreNoDialogueWarning, count, tagsHolder)
            register(event)
            return event
        }

        private val events = mutableMapOf<String, DialogueEvent>()

        fun getById(id: String): DialogueEvent? {
            return events[id]
        }

        private fun register(event: DialogueEvent) {
            val existing = events.put(event.id, event)
            if (existing != null) {
                throw IllegalStateException("Event $event already registered")
            }
        }
    }
}