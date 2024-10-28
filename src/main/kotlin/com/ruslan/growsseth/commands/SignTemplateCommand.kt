package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.ruslan.growsseth.templates.SignTemplates
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items
import kotlin.collections.forEach

object SignTemplateCommand {
    private val ERROR_SIGN_TEMPLATE_INVALID = DynamicCommandExceptionType {  Component.translatable("growsseth.commands.signtemplate.invalidtemplate", it) }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("signtemplate").requires{ it.hasPermission(2) }
            .then(
                argument("targets", EntityArgument.player())
                    .then(
                        argument("template", StringArgumentType.string())
                            .executes { ctx ->
                                giveTemplateSign(
                                    ctx.source,
                                    EntityArgument.getPlayers(ctx, "targets"),
                                    StringArgumentType.getString(ctx, "template")
                                )
                            })
            )
            .then(
                literal("list")
                    .executes { ctx -> listSignTemplates(ctx.source) }
            )
        )
    }

    private fun listSignTemplates(commandSourceStack: CommandSourceStack): Int {
        commandSourceStack.sendSuccess({
            Component.literal(
                SignTemplates.getAvailableTemplates().joinToString(", ")
            )
        }, true)
        return 1
    }

    private fun giveTemplateSign(commandSourceStack: CommandSourceStack, players: Collection<ServerPlayer>, templateName: String): Int {
        if (!SignTemplates.templateExists(templateName)) {
            throw ERROR_SIGN_TEMPLATE_INVALID.create(templateName)
        }

        var sign = Items.OAK_SIGN.defaultInstance
        sign = SignTemplates.loadTemplate(sign, templateName, players.random())

        players.forEach { player ->
            player.inventory.placeItemBackInInventory(sign)
            player.level().playSound(
                player,
                player.x, player.y, player.z,
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.2f,
                ((player.random.nextFloat() - player.random.nextFloat()) * 0.7f + 1.0f) * 2.0f
            )
            player.sendSystemMessage(Component.translatable("growsseth.commands.signtemplate.success", templateName))
        }

        return 1
    }
}