package com.ruslan.growsseth.dialogues

import com.mojang.datafixers.util.Either
import com.ruslan.growsseth.utils.serverLang
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*


/**
 * Note: for dialogues file format (including shorthands to avoid writing a full DialogueEntry object), see
 * javadoc for [ResearcherDialogueListener]
 *
 * @param content List of dialogue lines. In JSON can have different formats, see [DialogueLineStringsSerializer]
 * @param weight Float weight of the value for random choice, higher is easier to choose. Defaults to 1.
 * @param id String id to identify this dialogue entry, needed for shared dialogues and dialogues that have a useLimit
 *   (to identify their amount of usages)
 * @param useLimit Maximum amount of times this specific dialogue entry can be played, regardless of total event triggers.
 *   Requires an id to be set to allow storing the amount of triggers.
 * @param afterRepeatsMin Minimum amount of event triggers for the dialogue's event for the dialogue to play.
 * @param afterRepeatsMax As [afterRepeatsMin], but max amount (inclusive).
 * @param afterCloseRepeatsMin Minimum amount of event triggers within some time (defaults to a minute, configurable in entity)
 *   for the dialogue's event for the dialogue to play.
 * @param afterCloseRepeatsMax As [afterCloseRepeatsMin], but max amount (inclusive).
 * @param requiresQuest If this dialogue requires a quest (as per [com.ruslan.growsseth.quests] package content), specify quest name.
 *   If the entity only has one quest assigned in its class, can just specify the stage instead, this will default to the one quest
 *   it has. (Currently quests only support one per entity, but future might not)
 * @param requiresQuestStage As [requiresQuest], but also require a specific quest stage by its name. Meant for
 *   dialogues that trigger after that stage is triggered.
 * @param requiresUntilQuestStage As [requiresQuestStage], but disables the dialogue instead, at the specified stage.
 *   Meant for dialogues that shouldn't risk to be played if the quest is started, or the stage is surpassed.
 * @param requiresEventParam Requires a specific value of the [DialogueEvent] param, for example the name in a RENAME event.
 *   If the event has no param (in general or in that specific trigger) will not allow the dialogue.
 * @param immediate If true, immediately play the dialogue when triggered, skipping any queue or delay.
 * @param data Arbitrary string-string data object, to be used by specific entities in their own ways.
 *   (For example, an entity might have sound customizations here).
 * @param priority Int, defaults to 0. After filtering allowed dialogues with the other attributes, will keep only the
 *   ones with the highest priority for random choice. Set this to a higher value to force this to play if the conditions
 *   are met (avoid setting this if the dialogue can always play and has no limits).
 */
@Serializable
data class DialogueEntry(
    @Serializable(with = DialogueLineStringsSerializer::class)
    val content: List<DialogueLine>,
    val weight: Float = 1f,
    val id: String? = null,
    @Serializable(with = ListStringSerializer::class)
    val groups: List<String>? = null,
    val useLimit: Int? = null,
    val groupUseLimit: Int? = null,
    val afterRepeatsMin: Int = 1, //event-wide
    val afterRepeatsMax: Int? = null,
    val afterCloseRepeatsMin: Int = 1, //event-wide
    val afterCloseRepeatsMax: Int? = null,
    val requiresQuest: String? = null, // can be omitted even if stage set, will default to the one quest the npc has
    val requiresQuestStage: String? = null,
    val requiresUntilQuestStage: String? = null,
    val requiresEventParam: String? = null,
    val immediate: Boolean = false, // Skip the queue (for things like taking damage)
    val data: Map<String, String> = mapOf(),
    val priority: Int = 0, // higher -> has priority
) {
    init {
        assert(id != null || useLimit == null) {
            "Must set an id to track a useLimit" +
            "Were content:$content id:$id useLimit:$useLimit afterMinTimes:$afterCloseRepeatsMin afterMaxTimes:$afterCloseRepeatsMax"
        }

        content.forEach {
            it.dialogue = this
        }
    }

    companion object {
        fun ofKey(key: String): DialogueEntry {
            return DialogueEntry(content=listOf(DialogueLine(key=key)))
        }

        fun ofText(text: String): DialogueEntry {
            return DialogueEntry(content=listOf(DialogueLine(text=text)))
        }

        fun getAllForEvent(event: DialogueEvent): List<DialogueEntry> {
            val out = ResearcherDialogueListener.DIALOGUE_OPTIONS[event.id]?.toMutableList() ?:
                        mutableListOf()
            if (event in ResearcherDialogueApiListener.API_DIALOGUES_EVENTS) {
                out += ResearcherDialogueApiListener.API_DIALOGUES
            }
            return out
        }

        fun getWithId(id: String): DialogueEntry? {
            return ResearcherDialogueListener.BY_ID[id]
        }
    }
}

/**
 * @param key Key to the server lang string of the line. Must set either this or `text`. See FxLib for server lang.
 * @param text Hardcoded text of the line. Must set either this or `key`.
 * @param duration Optional duration in seconds, otherwise will be calculated by the dialogue component.
 *   (Usually by WPM).
 */
@Serializable
data class DialogueLine(
    val key: String? = null,
    val text: String? = null,
    val duration: Float? = null,
) {
    @Transient
    lateinit var dialogue: DialogueEntry
    @Transient
    val keyOrText: Either<String, String> = key?.let { Either.left(it) } ?: Either.right(text!!)

    fun content(): String {
        return keyOrText.map({key -> serverLang().getOrDefault(key) }, {it})
    }

    init {
        if (key == null && text == null) {
            throw IllegalArgumentException("Cannot have both key and text null!")
        }
        if (key != null && text != null) {
            throw IllegalArgumentException("Cannot have both key and text set!")
        }
    }
}

/**
 * [DialogueLine] after server-side processing (duration calculation, server localisation) is done
 */
@Serializable
data class DialogueLineProcessed(
    val text: String,
    val duration: Float,
) {
    @Transient
    lateinit var dialogue: DialogueEntry
}