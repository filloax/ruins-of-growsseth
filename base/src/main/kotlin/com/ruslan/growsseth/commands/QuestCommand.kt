package com.ruslan.growsseth.commands

import com.filloax.fxlib.api.alwaysTruePredicate
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.quests.QuestComponent
import com.ruslan.growsseth.quests.QuestOwner
import com.ruslan.growsseth.quests.QuestStage
import com.ruslan.growsseth.quests.QuestUpdateEvent
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import java.util.concurrent.CompletableFuture

object QuestCommand {
    private const val LANG_PREFIX = "growsseth.commands.gquest"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("gquest").requires{ it.hasPermission(2) }
            .then(
                literal("ent")
                    .then(argument("entity", EntityArgument.entity()).also {
                        arg -> registerEntityArgs(arg) {  ctx ->
                            EntityArgument.getEntity(ctx, "entity") as LivingEntity
                        }
                })
            )
            .then(
                literal("researcher").also {
                    arg -> registerEntityArgs(arg) {  ctx ->
                        val level = ctx.source.level
                        val pos = ctx.source.position
                        level.getNearestEntity(level.getEntities(GrowssethEntities.RESEARCHER, alwaysTruePredicate()), TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting(), null, pos.x, pos.y, pos.z)
                    }
                }
            )
        )
    }

    private fun <T : ArgumentBuilder<CommandSourceStack, T>> registerEntityArgs(builder: ArgumentBuilder<CommandSourceStack, T>, getEnt: (ctx: CommandContext<CommandSourceStack>) -> LivingEntity?) {
        builder.then(literal("stage")
            .then(argument("stageId", StringArgumentType.word())
                    .executes { ctx -> setStage(ctx, getEnt(ctx), StringArgumentType.getString(ctx, "stageId")) }
                )
            )
            .then(literal("back")
                .executes { ctx -> backStage(ctx, getEnt(ctx), false) }
                .then(literal("activate")
                    .then(argument("doActivate", BoolArgumentType.bool())
                        .executes { ctx -> backStage(ctx, getEnt(ctx), BoolArgumentType.getBool(ctx, "doActivate")) }))
            )
            .then(literal("list_stages")
                .executes { ctx -> listQuestStages(ctx, getEnt(ctx)) }
            )
            .then(literal("info")
                .then(literal("triggers")
                    .then(argument("event", QuestUpdateEventArgument.get()).executes { ctx ->
                        showStageTriggers(ctx, getEnt(ctx), QuestUpdateEventArgument.getEvent(ctx, "event"))
                    })
                    .executes { ctx -> showStageTriggers(ctx, getEnt(ctx)) })
                .executes { ctx -> showQuestInfo(ctx, getEnt(ctx)) }
            )
    }

    private fun setStage(ctx: CommandContext<CommandSourceStack>, targetEntity: Entity?, stage: String): Int {
        val quest = getQuest(ctx, targetEntity) ?: return 0

        return try {
            quest.activateStageId(stage)
            ctx.source.sendSuccess({ Component.translatable("$LANG_PREFIX.stage.success", stage, targetEntity?.name, quest.toString()) }, true)
            1
        } catch (_: IllegalArgumentException) {
            ctx.source.sendFailure(Component.translatable("$LANG_PREFIX.stage.notFound", stage, targetEntity?.name, quest.availableStages.joinToString(", ")))
            0
        } catch (e: Exception) {
            ctx.source.sendFailure(Component.translatable(e.message ?: "$LANG_PREFIX.stage.failure", stage, targetEntity.toString()))
            0
        }
    }

    private fun backStage(ctx: CommandContext<CommandSourceStack>, targetEntity: Entity?, activate: Boolean): Int {
        val quest = getQuest(ctx, targetEntity) ?: return 0

        return if (quest.data.stageHistory.size <= 1) {
            ctx.source.sendFailure(Component.translatable("$LANG_PREFIX.back.notAllowed", targetEntity?.name))
            0
        }
        else if (quest.backOneStage(activate)) {
            ctx.source.sendSuccess({ Component.translatable("$LANG_PREFIX.back.success", quest.data.currentStageId, targetEntity?.name, quest.toString()) }, true)
            1
        } else {
            ctx.source.sendFailure(Component.translatable("$LANG_PREFIX.back.failure", targetEntity.toString()))
            0
        }
    }

    private fun listQuestStages(ctx: CommandContext<CommandSourceStack>, targetEntity: Entity?): Int {
        val quest = getQuest(ctx, targetEntity) ?: return 0

        ctx.source.sendSuccess({ Component.translatable("$LANG_PREFIX.list_stages", targetEntity?.name, quest.availableStages.joinToString(", ")) }, true)
        return 1
    }

    private fun showQuestInfo(ctx: CommandContext<CommandSourceStack>, targetEntity: LivingEntity?): Int {
        val quest = getQuest(ctx, targetEntity) ?: return 0

        ctx.source.sendSuccess({ Component.translatable("${LANG_PREFIX}.info", targetEntity.toString(), quest.data.currentStageId, quest.toString()) }, true)
        return 1
    }

    private fun showStageTriggers(ctx: CommandContext<CommandSourceStack>, targetEntity: LivingEntity?, questUpdateEvent: QuestUpdateEvent = QuestUpdateEvent.TICK): Int {
        val quest = getQuest(ctx, targetEntity) ?: return 0

        if (targetEntity == null) {
            ctx.source.sendFailure(Component.translatable("$LANG_PREFIX.triggers.noent"))
            return 0
        }

        val nextStages = quest.currentNode.leadsTo
        val nextStagesInfo = nextStages.map { node ->
            @Suppress("UNCHECKED_CAST")
            val stage = node.stage as QuestStage<LivingEntity>
            val stageTriggerTreeStr = stage.getTriggerStatusTree(targetEntity, questUpdateEvent).toString()
                .replace("\n", "\n  ")
                .replace(Regex("^."), "  $0")
            Component.literal("${node.id}:\n").append(stageTriggerTreeStr)
        }.reduce { c, c2 -> if (c == null) c2 else c.append("\n").append(c2) }


        ctx.source.sendSuccess({
            Component.translatable("$LANG_PREFIX.info", targetEntity.toString(), quest.data.currentStageId, quest.toString())
                .append("\n")
                .append(Component.translatable("$LANG_PREFIX.info.triggers", questUpdateEvent.toString()))
                .append("\n")
                .append(nextStagesInfo)
        }, true)
        return 1
    }

    private fun getQuest(ctx: CommandContext<CommandSourceStack>, targetEntity: Entity?): QuestComponent<*>? {
        if (targetEntity == null) {
            // Error in chat is handled by the game
            return null
        }
        if (targetEntity !is QuestOwner<*> || targetEntity.quest == null) {
            ctx.source.sendFailure(Component.translatable("$LANG_PREFIX.noQuest", targetEntity.toString()))
            return null
        }
        return targetEntity.quest
    }

    class QuestUpdateEventArgument private constructor() : ArgumentType<QuestUpdateEvent> {
        companion object {
            fun getFromInput(input: String): QuestUpdateEvent = QuestUpdateEvent.valueOf(input)

            fun getEvent(context: CommandContext<CommandSourceStack>, name: String): QuestUpdateEvent {
                return context.getArgument(name, QuestUpdateEvent::class.java)
            }

            fun get() = QuestUpdateEventArgument()
        }

        override fun parse(reader: StringReader): QuestUpdateEvent {
            val input = reader.readString().trim()
            return getFromInput(input)
        }

        override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
            return SharedSuggestionProvider.suggest(QuestUpdateEvent.entries.map { it.toString() }, builder);
        }
    }

}