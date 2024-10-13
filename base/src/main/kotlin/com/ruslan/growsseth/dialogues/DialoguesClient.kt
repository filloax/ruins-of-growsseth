package com.ruslan.growsseth.dialogues

import com.filloax.fxlib.api.FxLibServices
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.networking.DialoguePacket
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component

private val client = Minecraft.getInstance()
private val platform = FxLibServices.platform

fun LocalPlayer.handleNpcDialogueLine(packet: DialoguePacket) {
    val nameComp = Component.literal("<").append(packet.senderName.copy().withStyle(ChatFormatting.YELLOW)).append("> ")
    val messageComp = nameComp.append(Component.translatable(packet.dialogueLine.content))

    if (platform.isDevEnvironment() || com.ruslan.growsseth.config.DebugConfig.debugNpcDialogues)
        RuinsOfGrowsseth.LOGGER.info("[Client] Received NPC dialogue: ${messageComp.string}")

    if (!com.ruslan.growsseth.config.ClientConfig.disableNpcDialogues)
        client.gui.chat.addMessage(messageComp)
}