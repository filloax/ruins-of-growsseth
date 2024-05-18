package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.ruslan.growsseth.templates.BookTemplates
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items

object BookTemplateCommand {
    private val ERROR_BOOK_TEMPLATE_INVALID = DynamicCommandExceptionType {  Component.translatable("growsseth.commands.booktemplate.invalidtemplate", it) }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("booktemplate").requires{ it.hasPermission(2) }
            .then(
                argument("targets", EntityArgument.player())
                .then(
                    argument("template", StringArgumentType.string())
                    .executes { ctx ->
                        giveTemplateBook(
                            ctx.source,
                            EntityArgument.getPlayers(ctx, "targets"),
                            StringArgumentType.getString(ctx, "template")
                        )
                    })
            )
            .then(
                literal("list")
                    .executes { ctx -> listBookTemplates(ctx.source) }
            )
        )
    }

    private fun listBookTemplates(commandSourceStack: CommandSourceStack): Int {
        commandSourceStack.sendSuccess({
            Component.literal(
                BookTemplates.getAvailableTemplates().joinToString(", ")
            )
        }, true)
        return 1
    }

    private fun giveTemplateBook(commandSourceStack: CommandSourceStack, players: Collection<ServerPlayer>, templateName: String): Int {
        if (!BookTemplates.templateExists(templateName)) {
            throw ERROR_BOOK_TEMPLATE_INVALID.create(templateName)
        }

        var book = Items.WRITTEN_BOOK.defaultInstance
        book = BookTemplates.loadTemplate(book, templateName)

        val blockPos = BlockPos.containing(commandSourceStack.position)
        val serverLevel = commandSourceStack.level

        players.forEach { player ->
            player.inventory.placeItemBackInInventory(book)
            player.level().playSound(
                player,
                player.x, player.y, player.z,
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.2f,
                ((player.random.nextFloat() - player.random.nextFloat()) * 0.7f + 1.0f) * 2.0f
            )
        }

        return 1
    }

}