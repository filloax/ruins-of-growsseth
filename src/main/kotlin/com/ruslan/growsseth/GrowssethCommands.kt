package com.ruslan.growsseth

import com.filloax.fxlib.registration.CommandsRegistrationHelper
import com.mojang.brigadier.CommandDispatcher
import com.ruslan.growsseth.commands.*
import com.ruslan.growsseth.commands.GrowssethPlaceCommand.RandomizableRotationArgument
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.core.Registry

object GrowssethCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        BookTemplateCommand.register(dispatcher, registryAccess, environment)
        GiveStructMapCommand.register(dispatcher, registryAccess, environment)
        GrowssethDiaryCommand.register(dispatcher, registryAccess, environment)
        ReloadRemoteCommand.register(dispatcher, registryAccess, environment)
        ResearcherResetCommand.register(dispatcher, registryAccess, environment)
        GrowssethPlaceCommand.register(dispatcher, registryAccess, environment)
    }

    object ArgumentTypes {
        fun registerArgumentTypes(registry: Registry<ArgumentTypeInfo<*,*>>) {
            CommandsRegistrationHelper.registerArgumentType(
                registry,
                "rotation_randomizable",
                RandomizableRotationArgument::class.java,
                SingletonArgumentInfo.contextFree { RandomizableRotationArgument() }
            )
        }
    }
}