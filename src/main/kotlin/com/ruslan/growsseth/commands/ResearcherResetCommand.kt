package com.ruslan.growsseth.commands

import com.filloax.fxlib.alwaysTruePredicate
import com.mojang.brigadier.CommandDispatcher
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.ResearcherSavedData
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
            .executes { ctx ->
                if (ResearcherConfig.singleResearcher) {
                    resetAll(ctx.source.server)
                    ctx.source.sendSuccess({ Component.translatable("growsseth.commands.greset.done") }, true)
                    1
                } else {
                    ctx.source.sendFailure(Component.translatable("growsseth.commands.greset.nop"))
                    0
                }
            }
        )
    }

    private fun resetAll(server: MinecraftServer) {
        val savedData = ResearcherSavedData.getPersistent(server)
        savedData.data = CompoundTag()
        savedData.name = null
        savedData.setDirty()

        server.allLevels.forEach { level ->
            level.getEntities(GrowssethEntities.RESEARCHER, alwaysTruePredicate()).forEach { researcher ->
                researcher.readSavedData(savedData)
            }
        }
    }
}