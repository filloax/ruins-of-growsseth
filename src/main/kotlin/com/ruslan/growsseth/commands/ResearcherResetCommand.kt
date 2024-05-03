package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.researcher.ResearcherSavedData
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.commands.Commands.literal
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component

object ResearcherResetCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("greset").requires{ it.hasPermission(2) }
            .executes { ctx ->
                if (ResearcherConfig.singleResearcher) {
                    val savedData = ResearcherSavedData.getPersistent(ctx.source.server)
                    savedData.data = CompoundTag()
                    savedData.name = null
                    savedData.setDirty()
                    ctx.source.sendSuccess({ Component.translatable("growsseth.commands.greset.done") }, true)
                    1
                } else {
                    ctx.source.sendFailure(Component.translatable("growsseth.commands.greset.nop"))
                    0
                }
            }
        )
    }
}