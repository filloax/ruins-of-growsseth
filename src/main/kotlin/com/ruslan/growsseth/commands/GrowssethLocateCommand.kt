package com.ruslan.growsseth.commands

import com.google.common.base.Stopwatch
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.locate.LocateTask
import com.ruslan.growsseth.structure.locate.SignalProgressFun
import com.ruslan.growsseth.structure.locate.StoppableAsyncLocator
import net.minecraft.Util
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.*
import net.minecraft.server.commands.LocateCommand
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.*
import kotlin.jvm.optionals.getOrNull

object GrowssethLocateCommand {
    private val ERROR_STRUCTURE_INVALID: DynamicCommandExceptionType = DynamicCommandExceptionType {
        Component.translatableEscape("commands.locate.structure.invalid", it)
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("glocate").requires { it.hasPermission(2) }
            .then(argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                .executes { ctx -> locateStructure(
                    ctx.source,
                    ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID),
                ) }
                .then(argument("timeout", IntegerArgumentType.integer(1))
                    .executes { ctx -> locateStructure(
                        ctx.source,
                        ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID),
                        IntegerArgumentType.getInteger(ctx, "timeout")
                    ) }
                    .then(argument("logProgress", BoolArgumentType.bool())
                        .executes { ctx -> locateStructure(
                            ctx.source,
                            ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID),
                            IntegerArgumentType.getInteger(ctx, "timeout"),
                            BoolArgumentType.getBool(ctx, "logProgress"),
                            BoolArgumentType.getBool(ctx, "logProgress"),
                        ) }
                    )
                )
            )
        )
    }

    @Throws(CommandSyntaxException::class)
    private fun locateStructure(source: CommandSourceStack, structure: ResourceOrTagKeyArgument.Result<Structure>, timeout: Int? = null, logProgress: Boolean = false, chatProgress: Boolean = false): Int {
        val registry = source.level.registryAccess().registryOrThrow(Registries.STRUCTURE)
        val holderSet = LocateCommand.getHolders(structure, registry).getOrNull() ?: throw ERROR_STRUCTURE_INVALID.create(structure.asPrintable())
        val blockPos = BlockPos.containing(source.position)
        val serverLevel = source.level
        val stopwatch = Stopwatch.createStarted(Util.TICKER)

        StoppableAsyncLocator.locate(
            serverLevel, holderSet, blockPos,
            100, false,
            timeout, getSignalProgress(source, structure, logProgress, chatProgress)
        ).thenOnServerThread {
            if (it == null) {
                source.sendFailure(Component.translatable("commands.locate.structure.invalid", structure.asPrintable()))
            } else {
                LocateCommand.showLocateResult(
                    source, structure, blockPos, it, "commands.locate.structure.success", false, stopwatch.elapsed()
                )
            }
        }.onExceptionOnServerThread {
            source.sendFailure(Component.literal("Error in async search: %s".format(it.message)))
        }

        return 1
    }

    private fun getSignalProgress(source: CommandSourceStack, structure: ResourceOrTagKeyArgument.Result<Structure>, logProgress: Boolean, chatProgress: Boolean): SignalProgressFun? {
        val chatProgressFun: SignalProgressFun = { task, phase, pct ->
            source.sendSystemMessage(Component.literal("Async Locate progress: %s %.2f%%, time %.2fs".format(phase, pct * 100, task.timeElapsedMs() / 1000.0)))
        }
        val str = structure.asPrintable()
        val logProgressFun: SignalProgressFun = { task, phase, pct ->
            RuinsOfGrowsseth.LOGGER.info("Async Locate $str progress: " + "%s %.2f%%, time %.2fs".format(phase, pct * 100, task.timeElapsedMs() / 1000.0))
        }
        if (chatProgress && logProgress) {
            return { task, phase, pct ->
                chatProgressFun(task, phase, pct)
                logProgressFun(task, phase, pct)
            }
        } else if (chatProgress) {
            return chatProgressFun
        } else if (logProgress) {
            return logProgressFun
        } else {
            return null
        }
    }
}