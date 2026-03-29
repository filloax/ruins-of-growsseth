package com.ruslan.growsseth.dialogues

import com.filloax.fxlib.api.FxLibServices
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ClientConfig
import com.ruslan.growsseth.config.DebugConfig
import com.ruslan.growsseth.network.DialoguePacket
import com.ruslan.growsseth.network.DialogueSeparatorPacket
import com.ruslan.growsseth.network.IDialoguePacket
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component

object DialoguesClient {
    private val client = Minecraft.getInstance()
    private val platform = FxLibServices.platform

    fun LocalPlayer.handleNpcDialogue(packet: IDialoguePacket) {
        when (packet) {
            is DialoguePacket -> handleDialogueLine(this, packet)
            is DialogueSeparatorPacket -> handleDialogueSeparator(this, packet)
        }
    }

    private fun handleDialogueLine(player: LocalPlayer, packet: DialoguePacket) {
        val nameComp = Component.literal("<").append(packet.senderName.copy().withStyle(ChatFormatting.YELLOW)).append("> ")
        val messageComp = nameComp.append(Component.translatable(packet.dialogueLine.text))

        if (platform.isDevEnvironment() || DebugConfig.debugNpcDialogues)
            RuinsOfGrowsseth.LOGGER.info("[Client] Received NPC dialogue: ${messageComp.string}")

        if (!ClientConfig.disableNpcDialogues)
            client.gui.chat.addClientSystemMessage(messageComp)
    }

    private fun handleDialogueSeparator(player: LocalPlayer, packet: DialogueSeparatorPacket) {
        val messageComp = Component.literal("*-------------------").withStyle(ChatFormatting.DARK_GRAY)
        if (!ClientConfig.disableNpcDialogues)
            client.gui.chat.addClientSystemMessage(messageComp)
    }
}