package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.ruslan.growsseth.templates.BookTemplates
import com.ruslan.growsseth.templates.SignTemplates
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.collections.forEach

object GrowssethTemplateCommand {
    private val ERROR_BOOK_TEMPLATE_INVALID = DynamicCommandExceptionType { Component.translatable("growsseth.commands.gtemplate.invalidbooktemplate", it) }
    private val ERROR_SIGN_TEMPLATE_INVALID = DynamicCommandExceptionType { Component.translatable("growsseth.commands.gtemplate.invalidsigntemplate", it) }

    enum class TemplateType { BOOK, SIGN }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("gtemplate").requires{ it.hasPermission(2) }
            .then(
                literal("book").also { arg -> registerTemplateArgs(arg, TemplateType.BOOK) }
            )
            .then(
                literal("sign").also { arg -> registerTemplateArgs(arg, TemplateType.SIGN) }
            )
        )
    }

    fun <T : ArgumentBuilder<CommandSourceStack, T>> registerTemplateArgs(builder: ArgumentBuilder<CommandSourceStack, T>, templateType: TemplateType) {
        builder
            .then(
                argument("targets", EntityArgument.player())
                .then(
                    argument("template", StringArgumentType.string())
                        .executes { ctx ->
                            giveTemplate(
                                ctx.source,
                                EntityArgument.getPlayers(ctx, "targets"),
                                StringArgumentType.getString(ctx, "template"),
                                templateType
                            )
                        })
            )
            .then(
                literal("list")
                    .executes { ctx -> listTemplates(ctx.source, templateType) }
            )
    }

    private fun listTemplates(commandSourceStack: CommandSourceStack, templateType: TemplateType): Int {
        commandSourceStack.sendSuccess( {
            Component.literal(
                when (templateType) {
                    TemplateType.BOOK -> BookTemplates.getAvailableTemplates().joinToString(", ")
                    TemplateType.SIGN -> SignTemplates.getAvailableTemplates().joinToString(", ")
                }
            )
        }, true)
        return 1
    }

    private fun giveTemplate(sourceStack: CommandSourceStack, players: Collection<ServerPlayer>, templateName: String, templateType: TemplateType): Int {
        var item: ItemStack

        when (templateType) {
            TemplateType.BOOK -> {
                if (!BookTemplates.templateExists(templateName)) {
                    throw ERROR_BOOK_TEMPLATE_INVALID.create(templateName)
                }
                item = Items.WRITTEN_BOOK.defaultInstance
                item = BookTemplates.loadTemplate(item, templateName)
            }
            TemplateType.SIGN -> {
                if (!SignTemplates.templateExists(templateName)) {
                    throw ERROR_SIGN_TEMPLATE_INVALID.create(templateName)
                }
                item = Items.OAK_SIGN.defaultInstance
                item = SignTemplates.loadTemplate(item, templateName, sourceStack.level.registryAccess())
            }
        }

        players.forEach { player ->
            player.inventory.placeItemBackInInventory(item)
            player.level().playSound(
                player,
                player.x, player.y, player.z,
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.2f,
                ((player.random.nextFloat() - player.random.nextFloat()) * 0.7f + 1.0f) * 2.0f
            )
            player.sendSystemMessage(Component.translatable("growsseth.commands.gtemplate.${templateType.toString().lowercase()}success", templateName))
        }

        return 1
    }
}