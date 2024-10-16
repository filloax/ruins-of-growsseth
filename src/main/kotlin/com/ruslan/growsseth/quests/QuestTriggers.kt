package com.ruslan.growsseth.quests

import com.filloax.fxlib.api.secondsToTicks
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.dialogues.DialoguesNpc
import com.ruslan.growsseth.http.GrowssethApi
import net.minecraft.core.SectionPos
import net.minecraft.world.entity.LivingEntity
import java.lang.IllegalArgumentException


fun interface QuestStageTrigger<E : LivingEntity> {
    /**
     * Main function to check if the quest stage should be unlocked
     * @return If the quest stage should be activated
     */
    fun isActive(entity: E, event: QuestUpdateEvent): Boolean

    infix fun and(other: QuestStageTrigger<E>): QuestStageTrigger<E> = andMulti(other)
    infix fun or(other: QuestStageTrigger<E>): QuestStageTrigger<E> = orMulti(other)

    fun andMulti(vararg with: QuestStageTrigger<E>): QuestStageTrigger<E> {
        return AndTrigger(with.toMutableList().also{it.add(this)})
    }

    fun orMulti(vararg with: QuestStageTrigger<E>): QuestStageTrigger<E> {
        return OrTrigger(with.toMutableList().also{it.add(this)})
    }
}

open class ApiEventTrigger<E : LivingEntity>(val apiEventName: String) :
    QuestStageTrigger<E> {
    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean {
        return GrowssethApi.current.isEventActive(apiEventName)
    }
}

class AlwaysTrueTrigger<E: LivingEntity> : QuestStageTrigger<E> {
    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean = true
}

/**
 * Trigger this quest only if the npc started this dialogue at least once,
 * uses dialogue id to identify it (so it must have it set in the json).
 */
class DialogueTrigger<E: LivingEntity>(private val dialogueId: String) : QuestStageTrigger<E> {
    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean {
        assert(entity is DialoguesNpc) { "DialogueTrigger must be used on DialoguesNPC owner" }
        val dialogues = (entity as DialoguesNpc).dialogues
            ?: throw IllegalStateException("Must be called on server side")
        return dialogues.getTriggeredDialogues().entries
            .find { it.key.id == dialogueId }
            ?.value?.let { it > 0} == true
    }
}

open class EventTrigger<E: LivingEntity>(vararg val forEvents: QuestUpdateEvent): QuestStageTrigger<E> {
    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean {
        return (forEvents.contains(event))
    }
}

class TimeTrigger<E : LivingEntity>(
    private val questComponent: QuestComponent<E>,
    val requiredTime: Long,
) : QuestStageTrigger<E> {
    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean {
        val time = entity.server!!.overworld().gameTime
        return questComponent.data.currentStageTriggerTime < 0
                || time - questComponent.data.currentStageTriggerTime >= requiredTime
    }
}

class DayTimeTrigger<E : LivingEntity>(
    private val questComponent: QuestComponent<E>,
    val requiredTime: Long,
) : QuestStageTrigger<E> {
    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean {
        val time = entity.server!!.overworld().dayTime
        val timeOff = time - questComponent.data.currentStageTriggerDayTime
        // Time set fuckery, reset trigger time
        if (timeOff < 0 || questComponent.data.currentStageTriggerDayTime < 0) {
            RuinsOfGrowsseth.LOGGER.warn("Time changed backwards (command?), resetting quest trigger time")
            questComponent.data.currentStageTriggerDayTime = time
            return false
        }
        return timeOff >= requiredTime
    }
}

fun <E : LivingEntity> TimeOrDayTimeTrigger(
    questComponent: QuestComponent<E>,
    requiredTime: Long,
) = TimeTrigger(questComponent, requiredTime).or(DayTimeTrigger(questComponent, requiredTime))

class NoPlayersInRadiusTrigger<E : LivingEntity>(
    private val quest: QuestComponent<E>,
    radius: Int? = null,
    chunkRadius: Int? = null,
): QuestStageTrigger<E> {
    val radius = radius ?: chunkRadius?.let{ it * SectionPos.SECTION_SIZE }
        ?: throw IllegalArgumentException("NoPlayersInRadiusTrigger needs radius or chunkRadius")

    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean {
        val updatePeriod = (5f.secondsToTicks() / quest.updatePeriod) * quest.updatePeriod // Ensure is multiple of updatePeriod
        if (entity.tickCount % updatePeriod == 0) {
            val hasPlayers = entity.level().getNearestPlayer(entity, radius.toDouble()) != null
            return !hasPlayers
        }
        return false
    }
}

private class AndTrigger<E: LivingEntity>(val parts: List<QuestStageTrigger<E>>): QuestStageTrigger<E> {
    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean {
        val out = parts.all {
            it.isActive(entity, event)
        }
        return out
    }
}

private class OrTrigger<E: LivingEntity>(val parts: List<QuestStageTrigger<E>>): QuestStageTrigger<E> {
    override fun isActive(entity: E, event: QuestUpdateEvent): Boolean {
        return parts.any { it.isActive(entity, event) }
    }
}