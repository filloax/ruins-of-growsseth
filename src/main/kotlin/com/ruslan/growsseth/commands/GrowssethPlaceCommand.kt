package com.ruslan.growsseth.commands

import com.filloax.fxlib.codec.decodeJsonNullable
import com.filloax.fxlib.structure.FixablePosition
import com.filloax.fxlib.structure.FixableRotation
import com.filloax.fxlib.structure.tracking.CustomPlacedStructureTracker
import com.google.gson.JsonPrimitive
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ResourceKeyArgument
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.OutgoingChatMessage.Disguised
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

/**
 * Variant of /place that allows to set rotation, if should use fixed position y for fxLib structs,
 * and registers structures with fxLib for /locate and similar
 */
object GrowssethPlaceCommand {
    private val ERROR_STRUCTURE_FAILED = SimpleCommandExceptionType(Component.translatable("commands.place.structure.failed"))
    private val ERROR_INVALID_ROTATION = SimpleCommandExceptionType(Component.translatable("growsseth.commands.gplace.rotation.wrong"))

    private const val RANDOM_ROTATION = "random"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(Commands.literal("gplace").requires { it.hasPermission(2)  }
            .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                .executes { commandContext ->
                    placeStructure(
                        commandContext.source,
                        ResourceKeyArgument.getStructure(commandContext, "structure"),
                        BlockPos.containing((commandContext.source).position),
                        null, null
                    )
                }.then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes { commandContext: CommandContext<CommandSourceStack> ->
                        placeStructure(
                            commandContext.source,
                            ResourceKeyArgument.getStructure(commandContext, "structure"),
                            BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                            null, null
                        )
                    }
                    .then(Commands.argument("rotation", RandomizableRotationArgument.randomizableRotation())
                        .executes { commandContext: CommandContext<CommandSourceStack> ->
                            placeStructure(
                                commandContext.source,
                                ResourceKeyArgument.getStructure(commandContext, "structure"),
                                BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                RandomizableRotationArgument.getRotation(commandContext, "rotation"), null
                            )
                        }
                        .then(Commands.argument("usesY", BoolArgumentType.bool())
                            .executes { commandContext: CommandContext<CommandSourceStack> ->
                                placeStructure(
                                    commandContext.source,
                                    ResourceKeyArgument.getStructure(commandContext, "structure"),
                                    BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                    RandomizableRotationArgument.getRotation(commandContext, "rotation"),
                                    BoolArgumentType.getBool(commandContext, "usesY")
                                )
                            }
                        )
                    )
                )
            )
        )
    }

    @Throws(CommandSyntaxException::class)
    fun placeStructure(
        source: CommandSourceStack, structureHolder: Holder.Reference<Structure>, pos: BlockPos,
        rotation: Rotation?, fixedPosUsesY: Boolean?,
    ): Int {
        val serverLevel = source.level
        val structure = structureHolder.value()
        val chunkGenerator = serverLevel.chunkSource.generator

        // Additional functionality: fixed pos
        if (structure is FixablePosition) {
            structure.setNextPlacePosition(pos, fixedPosUsesY)
            val chatType = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, source)
            val msg = Disguised(Component.translatable("fxlib.commands.placetweak.fixedpos", structureHolder.key().location().toString(), pos.x, pos.y, pos.z))
            source.sendChatMessage(msg, true, chatType)
        }
        if (structure is FixableRotation && rotation != null) {
            structure.setNextPlaceRotation(rotation)
        }
        // Additional functionality done

        val structureStart = structure.generate(
            source.registryAccess(), chunkGenerator, chunkGenerator.biomeSource, serverLevel.chunkSource.randomState(),
            serverLevel.structureManager, serverLevel.seed, ChunkPos(pos), 0, serverLevel
        ) {  true }
        if (!structureStart.isValid) {
            throw ERROR_STRUCTURE_FAILED.create()
        }
        val boundingBox = structureStart.boundingBox
        val chunkPos2 = ChunkPos(SectionPos.blockToSectionCoord(boundingBox.minX()), SectionPos.blockToSectionCoord(boundingBox.minZ()))
        val chunkPos22 = ChunkPos(SectionPos.blockToSectionCoord(boundingBox.maxX()), SectionPos.blockToSectionCoord(boundingBox.maxZ()))
        checkLoaded(serverLevel, chunkPos2, chunkPos22)
        ChunkPos.rangeClosed(chunkPos2, chunkPos22).forEach { chunkPos: ChunkPos ->
            structureStart.placeInChunk(
                serverLevel, serverLevel.structureManager(), chunkGenerator, serverLevel.getRandom(), BoundingBox(
                    chunkPos.minBlockX, serverLevel.minBuildHeight, chunkPos.minBlockZ,
                    chunkPos.maxBlockX, serverLevel.maxBuildHeight, chunkPos.maxBlockZ
                ),
                chunkPos,
            )
        }

        // Additional functionality 2: register struct
        CustomPlacedStructureTracker.get(serverLevel).registerStructure(structureStart, pos)
        // Additional functionality 2 end

        val string = structureHolder.key().location().toString()
        source.sendSuccess({ Component.translatable("commands.place.structure.success", string, pos.x, pos.y, pos.z) }, true )
        return 1
    }

    @Throws(CommandSyntaxException::class)
    private fun checkLoaded(level: ServerLevel, start: ChunkPos, end: ChunkPos) {
        if (ChunkPos.rangeClosed(start, end).filter { chunkPos: ChunkPos ->
                !level.isLoaded(
                    chunkPos.worldPosition
                )
            }.findAny().isPresent) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create()
        }
    }

    class RandomizableRotationArgument : ArgumentType<Optional<Rotation>> {
        companion object {
            fun getFromInput(input: String): Rotation? = if (input == RANDOM_ROTATION) {
                null
            } else {
                Rotation.CODEC.decodeJsonNullable(JsonPrimitive(input))
                    ?: throw ERROR_INVALID_ROTATION.create()
            }

            fun getRotation(context: CommandContext<CommandSourceStack>, name: String): Rotation? {
                return context.getArgument(name, Optional::class.java).getOrNull() as Rotation?
            }

            fun randomizableRotation() = RandomizableRotationArgument()
        }

        override fun parse(reader: StringReader): Optional<Rotation> {
            val input = reader.readString().trim()
            return Optional.ofNullable(getFromInput(input))
        }

        override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
            return SharedSuggestionProvider.suggest(Rotation.entries.map{it.serializedName}.plus(RANDOM_ROTATION), builder);
        }
    }
}