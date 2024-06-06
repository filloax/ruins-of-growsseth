package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.ruslan.growsseth.config.GrowssethConfigHandler
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.http.DataRemoteSync
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.net.MalformedURLException
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utility commands for server admins that want to manage gamemaster mode
 */
object GamemasterCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(
            literal("gmaster").requires{ it.hasPermission(2) }
                .then(literal("help")
                    .executes { ctx -> printHelp(ctx.source) }
                )
                .then(literal("status")
                    .executes { ctx -> printStatus(ctx.source) }
                )
                .then(literal("reload")
                    .executes { ctx -> reload(ctx.source) }
                )
                .then(literal("enable")
                    .executes { ctx -> setEnabled(ctx.source, true) }
                )
                .then(literal("disable")
                    .executes { ctx -> setEnabled(ctx.source, false) }
                )
                .then(literal("url")
                    .then(argument("webUrl", StringArgumentType.string())
                        .executes { ctx -> setUrl(ctx.source, StringArgumentType.getString(ctx, "webUrl")) }
                    )
                )
        )
    }

    private fun printHelp(source: CommandSourceStack): Int {
        source.sendSuccess({
            val wikiLink = Component.literal("https://github.com/filloax/ruins-of-growsseth/wiki/EN---Gamemaster-Mode").withStyle(ChatFormatting.UNDERLINE)
                .withStyle { style ->
                    style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/filloax/ruins-of-growsseth/wiki/EN-%E2%80%90-Gamemaster-Mode"))
                }
            Component.translatable("growsseth.commands.gmaster.help").append(wikiLink)
        }, false)
        return 1
    }

    private fun printStatus(source: CommandSourceStack): Int {
        if (!WebConfig.webDataSync) {
            source.sendSystemMessage(Component.translatable("growsseth.commands.gmaster.status_off"))
            return 0
        }
        val suffix = if (DataRemoteSync.lastSyncSuccessful) "ok" else "ko"
        source.sendSystemMessage(Component.translatable(
            "growsseth.commands.gmaster.status_on_$suffix",
            WebConfig.dataSyncUrl, DataRemoteSync.lastSyncSuccessful
        ))
        source.sendSystemMessage(Component.translatable(
            "growsseth.commands.gmaster.status_time",
            formatTime(DataRemoteSync.lastSuccessfulUpdateTime), formatTime(DataRemoteSync.lastUpdateTime)
        ))

        return 1
    }

    private fun formatTime(time: LocalDateTime?) =
        time?.let { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it) } ?: "-"

    private fun reload(source: CommandSourceStack): Int {
        if (WebConfig.webDataSync) {
            source.sendSystemMessage(Component.translatable("growsseth.commands.gmaster.reload_start"))
            DataRemoteSync.doSync(WebConfig.dataSyncUrl, source.server).thenAccept {
                if (it) {
                    source.sendSuccess({ Component.translatable("growsseth.commands.gmaster.reload_success") }, true)
                } else {
                    source.sendFailure(Component.translatable("growsseth.commands.gmaster.reload_failure"))
                }
            }
            return 1
        } else {
            source.sendFailure(Component.translatable("growsseth.commands.gmaster.disabled"))
            return 0
        }
    }

    private fun setEnabled(source: CommandSourceStack, value: Boolean): Int {
        WebConfig.webDataSync = value
        GrowssethConfigHandler.saveConfig()
        restartWebService()
        val suffix = if (value) "on" else "off"
        source.sendSuccess({ Component.translatable("growsseth.commands.gmaster.set_$suffix", WebConfig.dataSyncUrl).withStyle(ChatFormatting.BOLD) }, true)
        return 1
    }

    private fun setUrl(source: CommandSourceStack, url: String): Int {
        if (!isValidUrl(url)) {
            source.sendFailure(Component.translatable("growsseth.commands.gmaster.url_not_valid", url))
            return 0
        }
        WebConfig.dataSyncUrl = url
        GrowssethConfigHandler.saveConfig()
        restartWebService()
        source.sendSuccess({ Component.translatable("growsseth.commands.gmaster.url_set", url).withStyle(ChatFormatting.BOLD) }, true)
        return 1
    }

    private fun isValidUrl(urlString: String): Boolean {
        return try {
            val url = URI(urlString).toURL()
            (url.protocol == "http" || url.protocol == "https")
                    && url.host.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun restartWebService() {

    }
}