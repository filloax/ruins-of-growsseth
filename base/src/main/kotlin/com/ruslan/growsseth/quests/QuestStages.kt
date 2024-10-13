package com.ruslan.growsseth.quests

import net.minecraft.world.entity.LivingEntity


enum class QuestUpdateEvent {
    TICK,
    LOAD,
//    UNLOAD, // Had issues with moving between chunks
    ;
}

/**
 * A single quest stage, with unlock criteria, things to run after it gets unlocked, etc.
 * Intended use case is a new instance in case you need different "inner" parameters (used by
 * isActive etc.) but if there are more quest stages with the same conditions, use the same instance
 * more times
 */
interface QuestStage<E : LivingEntity> {
    /**
     * Quest trigger, in a separate class to be reusable,
     * will run the onActivated function after this happens.
     */
    val trigger: QuestStageTrigger<E>

    // Non-required action that executes after the quest is activated the first time.
    fun onActivated(entity: E) {}
    // Non-required action to run every step
    fun onStep(entity: E) {}
    // Non-required action to run every update (second or as set in Quest class)
    fun onUpdate(entity: E) {}

    companion object {
        fun <E : LivingEntity> blank(): QuestStage<E> {
            return object : QuestStage<E> {
                override val trigger = QuestStageTrigger<E> { _, _ -> false }
            }
        }
    }
}