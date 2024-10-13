package com.ruslan.growsseth.http

import com.filloax.fxlib.api.EventUtil
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.utils.notNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.server.MinecraftServer
import net.minecraft.util.StringUtil
import net.minecraft.world.phys.Vec3

object RemoteCommandExec {
    const val PREFIX = "cmd"
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun handleCommandExec(event: ApiEvent, server: MinecraftServer, api: GrowssethApi) {
        if (!com.ruslan.growsseth.config.WebConfig.remoteCommandExecution) {
            RuinsOfGrowsseth.LOGGER.warn("Received command event but disabled in config, ignoring! $event")
            return
        }

        val id = event.name.replace("$PREFIX/", "")
        val cmd = event.desc?.trim()
        val pos = event.pos

        if (StringUtil.isNullOrEmpty(id)) {
            RuinsOfGrowsseth.LOGGER.error("Command exec must have an id after 'cmd/'! $event")
            return
        }
        if (StringUtil.isNullOrEmpty(cmd)) {
            RuinsOfGrowsseth.LOGGER.error("Command exec must have a desc with the command! $event")
            return
        }

        EventUtil.runWhenServerStarted(server, true) { srv ->
            val savedData = GrowssethExtraEvents.EventsSavedData.get(srv)
            if (savedData.alreadyRan.contains(id)) {
                return@runWhenServerStarted
            }

            savedData.alreadyRan.add(id)
            savedData.setDirty()

            RuinsOfGrowsseth.LOGGER.info("Executing remote command $cmd")
            performCommand(cmd!!, server, pos?.let{ Vec3(it.x+0.5, it.y+0.5, it.z+0.5) })
            RuinsOfGrowsseth.LOGGER.info("Executed remote command $cmd")
        }
    }

    fun handleCommandMessage(message: String, server: MinecraftServer, responseSender: ResponseSender) {
        RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Received command message $message")

        if (!com.ruslan.growsseth.config.WebConfig.remoteCommandExecution) {
            RuinsOfGrowsseth.LOGGER.warn("Received command message but disabled in config, ignoring! $message")
            responseSender.sendFailure("config_disabled")
            return
        }

        val remoteCommandDto: RemoteCommandDto = try {
            json.decodeFromString(RemoteCommandDto.serializer(), message)
        } catch (e: Exception) {
            RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | Wrong command format: ${e.message}")
            e.printStackTrace()
            responseSender.sendFailure(e.message)
            return
        }

        val success = performCommand(remoteCommandDto.command, server)

        RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Executed command, success: $success")
        if (success) {
            responseSender.sendSuccess()
        } else {
            responseSender.sendFailure("command_exception")
        }
    }

    private fun performCommand(command: String, server: MinecraftServer, pos: Vec3? = null): Boolean {
        return try {
            val commandSourceStack = server.createCommandSourceStack().let {
                if (notNull(pos))
                    it.withPosition(pos)
                else
                    it
            }
            server.commands.performPrefixedCommand(commandSourceStack, command)
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            RuinsOfGrowsseth.LOGGER.error("Failed remote command $command: ${e.message}")
            false
        }
    }

    @Serializable
    private data class RemoteCommandDto (
        val command: String,
    )
}