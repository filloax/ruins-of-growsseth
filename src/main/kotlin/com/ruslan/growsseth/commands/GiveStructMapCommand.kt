package com.ruslan.growsseth.commands

import com.google.common.base.Stopwatch
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.ruslan.growsseth.maps.MapLocateContext
import com.ruslan.growsseth.maps.updateMapToStruct
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

object GiveStructMapCommand {
    private val ERROR_STRUCTURE_NOT_FOUND = DynamicCommandExceptionType {  Component.translatable("commands.locate.structure.not_found", it) }
    private val ERROR_STRUCTURE_INVALID = DynamicCommandExceptionType {  Component.translatable("commands.locate.structure.invalid", it) }
    private val ERROR_MAP_ITEM_INVALID = DynamicCommandExceptionType {  Component.translatable("growsseth.commands.givestructmap.invalidmap", it) }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("givestructmap").requires{ it.hasPermission(2) }
            .then(
                argument("targets", EntityArgument.player())
                .then(
                    argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                    .then(
                        argument("mapItem", ItemArgument.item(registryAccess))
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
                .ifLeft { mapStack.updateMapToStruct(serverLevel, it, MapLocateContext(blockPos, 100, scale = 3)) }
                .ifRight { mapStack.updateMapToStruct(serverLevel, it, MapLocateContext(blockPos, 100, scale = 3)) }
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
}