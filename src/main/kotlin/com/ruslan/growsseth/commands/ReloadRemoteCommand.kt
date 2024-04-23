package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.http.DataRemoteSync
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ReloadRemoteCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(
            literal("reloadremote")  // Per versione Cydo: no requisiti admin, scommentare poi .requires{ it.hasPermission(2) }
            .executes { ctx ->
                val source = ctx.source

                if (WebConfig.webDataSync) {
                    source.sendSystemMessage(Component.translatable("growsseth.commands.reloadremote.start"))
                    DataRemoteSync.doSync(WebConfig.dataSyncUrl, ctx.source.server).thenAccept {
                        if (it) {
                            source.sendSuccess(
                                { Component.translatable("growsseth.commands.reloadremote.success") }, true)
                        } else {
                            source.sendFailure(Component.translatable("growsseth.commands.reloadremote.failure"))
                        }
                    }
                    1
                } else {
                    source.sendFailure(Component.translatable("growsseth.commands.reloadremote.disabled"))
                    0
                }
            }
        )
    }
}