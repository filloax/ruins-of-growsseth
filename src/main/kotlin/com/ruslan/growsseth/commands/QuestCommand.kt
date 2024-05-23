package com.ruslan.growsseth.commands

import com.filloax.fxlib.api.alwaysTruePredicate
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.quests.QuestComponent
import com.ruslan.growsseth.quests.QuestOwner
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.ai.targeting.TargetingConditions

object QuestCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("gquest").requires{ it.hasPermission(2) }
            .then(
                literal("ent").then(argument("entity", EntityArgument.entity()).also { arg -> registerEntityArgs(arg) {  ctx ->
                    val ent = EntityArgument.getEntity(ctx, "entity")
                    if (ent is QuestOwner<*>) ent else null
                }})
            )
            .then(
                literal("researcher").also { arg -> registerEntityArgs(arg) {  ctx ->
                    val level = ctx.source.level
                    val pos = ctx.source.position
                    level.getNearestEntity(level.getEntities(GrowssethEntities.RESEARCHER, alwaysTruePredicate()), TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting(), null, pos.x, pos.y, pos.z)
                }}
            )
        )
    }

    private fun <T : ArgumentBuilder<CommandSourceStack, T>> registerEntityArgs(builder: ArgumentBuilder<CommandSourceStack, T>, getEnt: (ctx: CommandContext<CommandSourceStack>) -> QuestOwner<*>?) {
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
            .then(literal("info")
                .executes { ctx -> showQuestInfo(ctx, getEnt(ctx)) }
            )
    }

    private fun setStage(ctx: CommandContext<CommandSourceStack>, owner: QuestOwner<*>?, stage: String): Int {
        val quest = getQuest(ctx, owner) ?: return 0

        return try {
            quest.activateStageId(stage)
            ctx.source.sendSuccess({ Component.translatable("growsseth.commands.gquest.stage.success", stage, quest.toString()) }, true)
            1
        } catch (e: Exception) {
            ctx.source.sendFailure(Component.translatable(e.message ?: "growsseth.commands.gquest.stage.failure"))
            0
        }
    }

    private fun backStage(ctx: CommandContext<CommandSourceStack>, owner: QuestOwner<*>?, activate: Boolean): Int {
        val quest = getQuest(ctx, owner) ?: return 0

        return if (quest.backOneStage(activate)) {
            ctx.source.sendSuccess({ Component.translatable("growsseth.commands.gquest.back.success", quest.data.currentStageId, quest.toString()) }, true)
            1
        } else {
            ctx.source.sendFailure(Component.translatable("growsseth.commands.gquest.back.failure"))
            0
        }
    }

    private fun showQuestInfo(ctx: CommandContext<CommandSourceStack>, owner: QuestOwner<*>?): Int {
        val quest = getQuest(ctx, owner) ?: return 0

        ctx.source.sendSuccess({ Component.translatable("growsseth.commands.gquest.info", owner, quest.data.currentStageId, quest.toString()) }, true)
        return 1
    }

    private fun getQuest(ctx: CommandContext<CommandSourceStack>, owner: QuestOwner<*>?): QuestComponent<*>? {
        if (owner == null) {
            ctx.source.sendFailure(Component.translatable("growsseth.commands.gquest.noQuest"))
            return null
        }

        return owner.quest ?: run {
            ctx.source.sendFailure(Component.translatable("growsseth.commands.gquest.noQuest"))
            null
        }
    }
}