package com.ruslan.growsseth.commands

import com.filloax.fxlib.api.alwaysTruePredicate
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.ruslan.growsseth.advancements.StructureAdvancements
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.ResearcherSavedData
import com.ruslan.growsseth.entity.researcher.trades.ProgressResearcherTradesProvider
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.commands.Commands.literal
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

object ResearcherResetCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("greset").requires{ it.hasPermission(2) }
            .then(literal("researcher").executes { ctx ->
                if (ResearcherConfig.singleResearcher) {
                    resetResearcherData(ctx, ctx.source.server)
                    ctx.source.sendSuccess({ Component.translatable("growsseth.commands.greset.done") }, true)
                    1
                } else {
                    ctx.source.sendFailure(Component.translatable("growsseth.commands.greset.nop"))
                    0
                }
            })
            .then(literal("structures").executes { ctx ->
                if (ResearcherConfig.singleResearcher) {
                    resetStructureProgress(ctx, ctx.source.server)
                    ctx.source.sendSuccess({ Component.translatable("growsseth.commands.greset.done") }, true)
                    1
                } else {
                    ctx.source.sendFailure(Component.translatable("growsseth.commands.greset.nop"))
                    0
                }
            })
            .then(literal("all").executes { ctx ->
                if (ResearcherConfig.singleResearcher) {
                    resetAll(ctx, ctx.source.server)
                    ctx.source.sendSuccess({ Component.translatable("growsseth.commands.greset.done") }, true)
                    1
                } else {
                    ctx.source.sendFailure(Component.translatable("growsseth.commands.greset.nop"))
                    0
                }
            })
            .executes { ctx ->
                if (ResearcherConfig.singleResearcher) {
                    resetAll(ctx, ctx.source.server)
                    ctx.source.sendSuccess({ Component.translatable("growsseth.commands.greset.done") }, true)
                    1
                } else {
                    ctx.source.sendFailure(Component.translatable("growsseth.commands.greset.nop"))
                    0
                }
            }
        )
    }

    private fun resetResearcherData(ctx: CommandContext<CommandSourceStack>, server: MinecraftServer) {
        val savedData = ResearcherSavedData.getPersistent(server)
        savedData.data = CompoundTag()
        savedData.name = null
        savedData.isDead = false
        savedData.setDirty()

        try {
            server.allLevels.forEach { level ->
                level.getEntities(GrowssethEntities.RESEARCHER, alwaysTruePredicate()).forEach { researcher ->
                    researcher.readSavedData(savedData)
                }
            }
        } catch(e: Exception) {
            ctx.source.sendFailure(Component.literal("Error in resetting loaded researchers: ${e.message}"))
            e.printStackTrace()
        }
    }

    private fun resetStructureProgress(ctx: CommandContext<CommandSourceStack>, server: MinecraftServer) {
        val progressData = ProgressResearcherTradesProvider.ProgressTradesSavedData.get(server)
        progressData.foundStructures.clear()
        progressData.setDirty()

        val structureFoundAdvancements = StructureAdvancements.getAllStructureAdvancementIds()
            .map { server.advancements.get(it) ?: run {
                ctx.source.sendFailure(Component.literal("Server didn't have all required advancements, missing $it"))
                return
            } }

        ctx.source.player?.let { player ->
            structureFoundAdvancements.forEach { adv ->
                val progress = player.advancements.getOrStartProgress(adv)

                for (string in progress.getCompletedCriteria()) {
                    player.advancements.revoke(adv, string)
                }
            }
        } ?: run {
            ctx.source.sendSystemMessage(Component.literal("Warning: couldn't reset player advancements as run not from player, researcher structure progress won't be completely reset").withStyle(ChatFormatting.YELLOW))
        }
    }

    private fun resetAll(ctx: CommandContext<CommandSourceStack>, server: MinecraftServer) {
        resetResearcherData(ctx, server)
        resetStructureProgress(ctx, server)
    }
}