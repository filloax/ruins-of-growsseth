package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.entity.researcher.Researcher
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB

object GrowssethDiaryCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(literal("gdiary").requires{ it.hasPermission(2) }
            .then(
                argument("name", StringArgumentType.string())
                .then(
                    argument("content", StringArgumentType.string())
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
    }

    private fun addDiaryNearby(ctx: CommandContext<CommandSourceStack>, name: String, content: String): Int {
        val pos = ctx.source.position
        val level = ctx.source.level
        val searchRange = 25.0
        val researchers = level.getEntitiesOfClass(
            Researcher::class.java, AABB.ofSize(pos, searchRange, searchRange, searchRange))
        researchers.firstOrNull()?.let {
            RuinsOfGrowsseth.LOGGER.info("[gdiary] Adding diary to researcher $it")
            it.diary?.makeArbitraryDiary(name, content)
            ctx.source.sendSuccess({ Component.translatable("growsseth.commands.gdiary.success") }, true)
            return 1
        }
        ctx.source.sendFailure(Component.translatable("growsseth.commands.gdiary.failure"))
        return 0
    }
}