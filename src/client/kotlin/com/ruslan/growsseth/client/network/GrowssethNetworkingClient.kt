package com.ruslan.growsseth.client.network

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.client.gui.components.NewTradeToast.Companion.updateNewTradeToast
import com.ruslan.growsseth.client.gui.components.updateCustomToast
import com.ruslan.growsseth.client.worldpreset.GrowssethWorldPresetClient
import com.ruslan.growsseth.config.ClientConfig
import com.ruslan.growsseth.config.GrowssethConfig
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.dialogues.handleNpcDialogueLine
import com.ruslan.growsseth.networking.*
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents

object GrowssethNetworkingClient {
    fun init() {
        val client = Minecraft.getInstance()

        ClientPlayNetworking.registerGlobalReceiver(DialoguePacket.TYPE) { packet, context ->
            client.player?.handleNpcDialogueLine(packet)
        }

        ClientPlayNetworking.registerGlobalReceiver(ResearcherTradesNotifPacket.TYPE) { packet, context ->
            if (ClientConfig.newTradeNotifications) {
                client.toasts.updateNewTradeToast(packet.newTrades)
                RuinsOfGrowsseth.LOGGER.info("Received trade notification")
            } else {
                RuinsOfGrowsseth.LOGGER.info("Ignoring trade notification as disabled client-side")
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(CustomToastPacket.TYPE) { packet, context ->
            client.toasts.updateCustomToast(packet.title, packet.message, packet.item)
        }

        ClientPlayNetworking.registerGlobalReceiver(StopMusicPacket.TYPE) { packet, context ->
            client.submit {
                client.musicManager.stopPlaying()
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(AmbientSoundsPacket.TYPE) { packet, context ->
            client.submit {
                client.player?.let { player ->
                    val simpleSoundInstance = SimpleSoundInstance.forAmbientMood(
                        SoundEvents.AMBIENT_CAVE.value(),
                        player.random,
                        player.x,
                        player.eyeY,
                        player.z
                    )
                    client.soundManager.play(simpleSoundInstance)
                }
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(PlacesInfoPacket.TYPE) { packet, context ->
            client.submit {
                GrowssethWorldPresetClient.initLocationData(packet.locationData)
            }
        }
    }
}