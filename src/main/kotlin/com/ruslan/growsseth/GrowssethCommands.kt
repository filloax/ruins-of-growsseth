package com.ruslan.growsseth

import com.google.common.base.Stopwatch
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.ruslan.growsseth.config.GrowssethConfig
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ResearcherSavedData
import com.ruslan.growsseth.http.DataRemoteSync
import com.ruslan.growsseth.maps.updateMapToStruct
import com.ruslan.growsseth.structure.StructureBooks
import com.sun.jdi.connect.Connector.StringArgument
import net.minecraft.Util
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument
import net.minecraft.commands.arguments.item.ItemArgument
import net.minecraft.commands.arguments.item.ItemInput
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.phys.AABB

object GrowssethCommands {
    private val ERROR_STRUCTURE_NOT_FOUND = DynamicCommandExceptionType {  Component.translatable("commands.locate.structure.not_found", it) }
    private val ERROR_STRUCTURE_INVALID = DynamicCommandExceptionType {  Component.translatable("commands.locate.structure.invalid", it) }
    private val ERROR_MAP_ITEM_INVALID = DynamicCommandExceptionType {  Component.translatable("growsseth.commands.givestructmap.invalidmap", it) }
    private val ERROR_BOOK_TEMPLATE_INVALID = DynamicCommandExceptionType {  Component.translatable("growsseth.commands.booktemplate.invalidtemplate", it) }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        // givestructmap
        dispatcher.register(literal("givestructmap").requires{ it.hasPermission(2) }
            .then(argument("targets", EntityArgument.player())
                .then(argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                    .then(argument("mapItem", ItemArgument.item(registryAccess))
                        .executes {
                            giveMapToStruct(
                                it.source,
                                EntityArgument.getPlayers(it, "targets"),
                                ResourceOrTagKeyArgument.getResourceOrTagKey(
                                    it, "structure",
                                    Registries.STRUCTURE, ERROR_STRUCTURE_NOT_FOUND,
                                ),
                                ItemArgument.getItem(it, "mapItem"),
                            )
                        }
                    )
                    .executes {
                        giveMapToStruct(
                            it.source,
                            EntityArgument.getPlayers(it, "targets"),
                            ResourceOrTagKeyArgument.getResourceOrTagKey(
                                it, "structure",
                                Registries.STRUCTURE, ERROR_STRUCTURE_NOT_FOUND,
                            ),
                        )
                    }
                )
            )
        )

        //reloadremote
        dispatcher.register(literal("reloadremote")  // Per versione Cydo: no requisiti admin, scommentare poi .requires{ it.hasPermission(2) }
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

        //greset: reset researcher
        dispatcher.register(literal("greset").requires{ it.hasPermission(2) }
            .executes { ctx ->
                if (ResearcherConfig.persistentResearcher) {
                    val savedData = ResearcherSavedData.getContainer(ctx.source.server)
                    val data = savedData.items
                    val num = data.size
                    data.clear()
                    savedData.setDirty()
                    ctx.source.sendSuccess({ Component.translatable("growsseth.commands.greset.done", num) }, true)
                    num
                } else {
                    ctx.source.sendFailure(Component.translatable("growsseth.commands.greset.nop"))
                    0
                }
            }
        )

        //gdiary: add diary to nearby researchers
        dispatcher.register(literal("gdiary").requires{ it.hasPermission(2) }
            .then(argument("name", StringArgumentType.string())
                .then(argument("content", StringArgumentType.string())
                    .executes {ctx ->
                        addDiaryNearby(ctx, StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "content"))
                    }
                )
                .executes { ctx ->
                    addDiaryNearby(ctx, StringArgumentType.getString(ctx, "name"), "Test diary content===Page 2")
                }
            )
            .executes { ctx ->
                addDiaryNearby(ctx, "Test Diary", "Test diary content===Page 2")
            }
        )

        //booktemplate: give book template
        dispatcher.register(literal("booktemplate").requires{ it.hasPermission(2) }
            .then(argument("targets", EntityArgument.player())
                .then(argument("template", StringArgumentType.string())
                    .executes { ctx ->
                    giveTemplateBook(
                        ctx.source,
                        EntityArgument.getPlayers(ctx, "targets"),
                        StringArgumentType.getString(ctx, "template")
                    )
                })
            )
            .executes { ctx -> listBookTemplates(ctx.source) }
        )
    }

    private fun giveMapToStruct(commandSourceStack: CommandSourceStack, players: Collection<ServerPlayer>, structureArg: ResourceOrTagKeyArgument.Result<Structure>, mapItemInput: ItemInput? = null): Int {
        val mapStack = mapItemInput?.let {
            if (it.item !is MapItem) {
                throw ERROR_MAP_ITEM_INVALID.create(it.item)
            }
            it.createItemStack(1, false)
        } ?: ItemStack(Items.FILLED_MAP)

        val blockPos = BlockPos.containing(commandSourceStack.position)
        val serverLevel = commandSourceStack.level

        val stopwatch = Stopwatch.createStarted(Util.TICKER)
        // Call the thing with either struct tag or key
        try {
            structureArg.unwrap()
                .ifLeft { updateMapToStruct(mapStack, serverLevel, it, blockPos, 100, scale = 3) }
                .ifRight { updateMapToStruct(mapStack, serverLevel, it, blockPos, 100, scale = 3) }
        } catch (e: Exception) {
            throw ERROR_STRUCTURE_INVALID.create(structureArg)
        }
        stopwatch.stop()

        players.forEach { player ->
            player.inventory.placeItemBackInInventory(mapStack)
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

    private fun addDiaryNearby(ctx: CommandContext<CommandSourceStack>, name: String, content: String): Int {
        val pos = ctx.source.position
        val level = ctx.source.level
        val searchRange = 25.0
        val researchers = level.getEntitiesOfClass(Researcher::class.java, AABB.ofSize(pos, searchRange, searchRange, searchRange))
        researchers.firstOrNull()?.let {
            RuinsOfGrowsseth.LOGGER.info("[gdiary] Adding diary to researcher $it")
            it.diary?.makeArbitraryDiary(name, content)
            ctx.source.sendSuccess({ Component.translatable("growsseth.commands.gdiary.success") }, true)
            return 1
        }
        ctx.source.sendFailure(Component.translatable("growsseth.commands.gdiary.failure"))
        return 0
    }

    private fun listBookTemplates(commandSourceStack: CommandSourceStack): Int {
        commandSourceStack.sendSuccess({
            Component.literal(
                StructureBooks.getAvailableTemplates().joinToString(", ")
            )
        }, true)
        return 1
    }

    private fun giveTemplateBook(commandSourceStack: CommandSourceStack, players: Collection<ServerPlayer>, templateName: String): Int {
        if (!StructureBooks.templateExists(templateName)) {
            throw ERROR_BOOK_TEMPLATE_INVALID.create(templateName)
        }

        var book = Items.WRITTEN_BOOK.defaultInstance
        book = StructureBooks.loadTemplate(book, templateName)

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