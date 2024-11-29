package com.ruslan.growsseth.network

import com.filloax.fxlib.api.networking.ToClientContext
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.client.gui.components.NewTradeToast.Companion.updateNewTradeToast
import com.ruslan.growsseth.client.gui.components.updateCustomToast
import com.ruslan.growsseth.client.worldpreset.GrowssethWorldPresetClient
import com.ruslan.growsseth.config.ClientConfig
import com.ruslan.growsseth.dialogues.handleNpcDialogueLine
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents

object ClientPacketHandlers {
    fun handleDialogue(packet: DialoguePacket, context: ToClientContext) {
        context.player.handleNpcDialogueLine(packet)
    }

    fun handleTradeNotification(packet: ResearcherTradesNotifPacket, context: ToClientContext) {
        if (ClientConfig.newTradeNotifications) {
            context.client.toasts.updateNewTradeToast(packet.newTrades)
            RuinsOfGrowsseth.LOGGER.info("Received trade notification")
        } else {
            RuinsOfGrowsseth.LOGGER.info("Ignoring trade notification as disabled client-side")
        }
    }

    fun handleCustomToast(packet: CustomToastPacket, context: ToClientContext) {
        context.client.toasts.updateCustomToast(packet.title, packet.message, packet.item)
    }

    fun handleStopMusic(packet: StopMusicPacket, context: ToClientContext) {
        context.client.submit {
            context.client.musicManager.stopPlaying()
        }
    }

    fun handleAmbientSounds(packet: AmbientSoundsPacket, context: ToClientContext) {
        context.client.submit {
            context.client.player?.let { player ->
                val simpleSoundInstance = SimpleSoundInstance.forAmbientMood(
                    SoundEvents.AMBIENT_CAVE.value(),
                    player.random,
                    player.x,
                    player.eyeY,
                    player.z
                )
                context.client.soundManager.play(simpleSoundInstance)
            }
        }
    }

    fun handlePlacesInfo(packet: PlacesInfoPacket, context: ToClientContext) {
        context.client.submit {
            GrowssethWorldPresetClient.initLocationData(packet.locationData)
        }
    }
}