package com.ruslan.growsseth.quests

import com.filloax.fxlib.*
import com.filloax.fxlib.nbt.*
import com.filloax.fxlib.codec.*
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.utils.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.lang.IllegalArgumentException
import java.util.*

interface QuestOwner<E : LivingEntity> {
    val quest: QuestComponent<E>?
}

/**
 * Class for tracking a "quest" regarding an entity,
 * could even be expanded to be centered on different/more
 * things than one entity later.
 */
open class QuestComponent<E : LivingEntity>(val entity: E, val name: String) {
    companion object {
        // For NBT persisting
        val PERSIST_CODEC: Codec<QuestData> = RecordCodecBuilder.create { builder ->
            builder.group(
                Codec.STRING.fieldOf("currentStageId").forGetter(QuestData::currentStageId),
                mutableListCodec(Codec.STRING).fieldOf("stageHistory").forGetter(QuestData::stageHistory),
                Codec.BOOL.fieldOf("active").forGetter(QuestData::active),
                Codec.LONG.optionalFieldOf("currentStageTriggerTime", -1).forGetter(QuestData::currentStageTriggerTime),
            ).apply(builder, ::QuestData)
        }

        const val INIT_STAGE_ID = "init"
        const val QUESTS_TAG_ID = "Quests"

        const val NBT_TAG_PERSIST = "status"
    }

    var updatePeriod = secondsToTicks(1f)

    private val stagesGraph = QuestNodeImpl(QuestStage.blank<E>(), INIT_STAGE_ID)
    private val stagesMap = mutableMapOf<String, QuestNodeImpl<E>>("init" to stagesGraph)
    private var triggeredFirst = false
    val server = entity.server ?: throw IllegalStateException("Initialized ResearcherQuestComponent in client!")
    val serverLevel
        get() = entity.level() as ServerLevel

    var data = QuestData()
        private set

    // NBT data
    data class QuestData(
        var currentStageId: String = INIT_STAGE_ID,
        var stageHistory: MutableList<String> = mutableListOf(INIT_STAGE_ID),
        var active: Boolean = true,
        var currentStageTriggerTime: Long = -1,
    )

    /**
     * Add stage to the stages graph, and add relevant connections between the various nodes
     * depending on starting points of this and previous stages
     * @param id id of the new stage node
     * @param stage Stage object with game logic
     * @param previousStage Stages nodes this stage can start from, defaults to initial if blank
     * @param priority Priority to check this stage in during activation checks, lower goes earlier
     * @param blockSiblingStages if true, this stage being activated will stop other stages with lower priority from being
     *  checked in the same iteration
     * @param blockNextStages if true, won't check following stages in the same iteration this stage is activated in
     */
    open fun addStage(
        id: String, stage: QuestStage<E>,
        vararg previousStage: String,
        priority: Int = 0,
        blockSiblingStages: Boolean = false,
        blockNextStages: Boolean = false
    ): Boolean {
        if (stagesMap.containsKey(id)) {
            RuinsOfGrowsseth.LOGGER.error("Added duplicate quest stage $id")
            return false
        }

        val node = QuestNodeImpl(stage, id, priority = priority, blockSiblings = blockSiblingStages, blockNext = blockNextStages)
        val startPoints = if (previousStage.isNotEmpty())
            previousStage.map { stagesMap[it] ?: throw IllegalArgumentException("Parent node $it not added yet / not existing") }
        else
            listOf(stagesGraph)
        node.startPoints.addAllByPriority(startPoints, QuestNode<E>::priority)

        stagesMap[id] = node

        startPoints.forEach {
            it.leadsTo.addByPriority(node, QuestNode<E>::priority)
        }

        return true
    }

    fun getStageNode(id: String): QuestNode<E>? {
        return stagesMap[id]
    }

    private fun current(): QuestNode<E> {
        return stagesMap.getOrElse(data.currentStageId) {
            RuinsOfGrowsseth.LOGGER.error("Unknown stage ${data.currentStageId}")
            data.currentStageId = INIT_STAGE_ID
            stagesGraph
        }
    }

    fun aiStep() {
        if (!data.active) return

        if ( entity.tickCount % updatePeriod == 0 || !triggeredFirst) {
            triggeredFirst = true
            update()
        }

        val current = current()
        current.stage.onStep(entity)
    }

    open fun update(event: QuestUpdateEvent = QuestUpdateEvent.TICK) {
        if (!data.active) return

        var current = current()

        do {
            val startCurrent = current
            for (node in startCurrent.leadsTo) {
                val stage = node.stage
                if (stage.trigger.isActive(entity, event)) {
                    current = activateStageId(node.id, node, event)
                    if (node.blockSiblings) break
                    if (entity.isRemoved) break
                }
            }
        } while (startCurrent != current && !current.blockNext && !entity.isRemoved)

        current.stage.onUpdate(entity)
    }

    fun activateStageId(id: String, node: QuestNode<E> = stagesMap[id] ?: throw IllegalArgumentException("No node specified and no node with id $id"),
                        event: QuestUpdateEvent?=null
    ): QuestNode<E> {
        data.currentStageId = id
        data.stageHistory += id
        data.currentStageTriggerTime = server.overworld().gameTime
        RuinsOfGrowsseth.LOGGER.info("Triggered stage ${node.id}${event?.let{" [$it]"} ?: ""}\n\t$this")
        node.stage.onActivated(entity)

        return node
    }

    fun backOneStage(activate: Boolean = false): Boolean {
        data.currentStageId = data.stageHistory.removeLastOrNull() ?: return false
        data.currentStageTriggerTime = server.overworld().gameTime
        RuinsOfGrowsseth.LOGGER.info("Reverted to quest stage ${data.currentStageId}\n\t$this")
        if (activate) {
            getStageNode(data.currentStageId)?.stage?.onActivated(entity) ?: throw IllegalStateException("No node for previous stage ${data.currentStageId}")
        }

        return true
    }

    fun started(): Boolean {
        return data.currentStageId != INIT_STAGE_ID
    }

    fun passedStage(id: String): Boolean {
        return data.stageHistory.contains(id)
    }

    open fun writeCustomNbt(tag: CompoundTag) {}
    open fun readCustomNbt(tag: CompoundTag) {}

    fun writeNbt(tag: CompoundTag) {
        val questsTag = tag.getOrPut(QUESTS_TAG_ID, CompoundTag())
        questsTag.put(name, CompoundTag().also { qTag ->
            qTag.put(NBT_TAG_PERSIST, PERSIST_CODEC.encodeNbt(data).getOrThrow(throwableCodecErr("QuestComponent write")))
            writeCustomNbt(qTag)
        })
    }

    fun readNbt(tag: CompoundTag) {
        data = QuestData()
        tag.getCompound(QUESTS_TAG_ID)?.let { questsTag ->
            questsTag.getCompound(name)?.let { qTag ->
                val result = PERSIST_CODEC.decodeNbt(qTag.getCompound(NBT_TAG_PERSIST)).result()
                result.ifPresent {
                    data = it.first
                    readCustomNbt(qTag)
                }
                if (result.isEmpty) RuinsOfGrowsseth.LOGGER.error("Couldn't parse quest data: $qTag")
            }
        }
    }

    interface QuestNode<E : LivingEntity> {
        val stage: QuestStage<E>
        val id: String
        val startPoints: List<QuestNode<E>>
        val leadsTo: List<QuestNode<E>>
        val priority: Int
        val blockSiblings: Boolean
        val blockNext: Boolean
    }

    data class QuestNodeImpl<E : LivingEntity> (
        override val stage: QuestStage<E>,
        override val id: String,
        override val startPoints: MutableList<QuestNode<E>> = mutableListOf(),
        override val leadsTo: MutableList<QuestNode<E>> = mutableListOf(),
        override val priority: Int = 0,
        override val blockSiblings: Boolean = false,
        override val blockNext: Boolean = false,
    ): QuestNode<E> {
        override fun toString(): String {
            var extra = ""
            if (blockSiblings) extra += ", block"
            if (blockNext) extra += ", blockNext"
            return "QuestNodeImpl[$id]<from: ${startPoints.map{it.id}}; to: ${leadsTo.map{it.id}}, $priority$extra>"
        }
    }

    override fun toString(): String {
        return data.stageHistory.joinToString(" -> ")
    }
}

object QuestComponentEvents {
    // Surround with try-catch to avoid messing with server loading
    fun onLoadEntity(entity: Entity) {
        try {
            if (entity is QuestOwner<*>) {
                entity.quest?.update(QuestUpdateEvent.LOAD)
            }
        } catch(e: Exception) {
            RuinsOfGrowsseth.LOGGER.error(e)
        }
    }
}