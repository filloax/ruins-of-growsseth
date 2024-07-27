package com.ruslan.growsseth.item

import com.filloax.fxlib.api.FxUtils
import com.ruslan.growsseth.sound.GrowssethSounds
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.JukeboxSong

object GrowssethJukeboxSongs {
    val SEGA_DI_NIENTE 		    = create("sega_di_niente")
    val GIORGIO_CUBETTI 		= create("giorgio_cubetti")
    val GIORGIO_LOFI 			= create("giorgio_lofi")
    val GIORGIO_LOFI_INST 		= create("giorgio_lofi_inst")
    val GIORGIO_FINDING_HOME 	= create("giorgio_finding_home")
    val GIORGIO_8BIT		 	= create("giorgio_8bit")
    val BINOBINOOO 			    = create("binobinooo")
    val PADRE_MAMMONK 			= create("padre_mammonk")
    val ABBANDONATI 			= create("abbandonati")
    val MISSIVA_NELL_OMBRA 	    = create("missiva_nell_ombra")
    val OURSTEPS 				= create("oursteps")
    val ODI_ET_AMO 			    = create("odi_et_amo")
    val ANCORA_QUI 			    = create("ancora_qui")
    val BALLATA_DEL_RESPAWN 	= create("ballata_del_respawn")
    val CACO_CACO 				= create("caco_caco")
    val PESCI_STRANI 			= create("pesci_strani")

    private fun create(id: String) = ResourceKey.create(Registries.JUKEBOX_SONG, resLoc(id))

    // Extended credits
    @JvmField
    val credits = mapOf(
        SEGA_DI_NIENTE to Credits(listOf(
                "Vocals & Lyrics - Cydonia",
                "Mixing - Guber",
                "Cover of 'Labyrinthine', by Lena Raine"
            )),
        GIORGIO_CUBETTI to Credits(listOf(
                "Vocals, Mix & Master - Singalek",
                "Lyrics - Singalek, Cydonia",
                "Cover of 'If I Could, I Would' - MINDME"
            ),
            ytChannel = "SingalekSMW"
        ),
        GIORGIO_LOFI to Credits(listOf(
                "Instrumental, Vocals, Mix, Master - Singalek",
                "Lyrics - Singalek, Cydonia"
            ),
            ytChannel = "SingalekSMW"
        ),
        GIORGIO_LOFI_INST to Credits(ytChannel = "SingalekSMW"),
        GIORGIO_FINDING_HOME to Credits(ytChannel = "HunterProduction"),
        GIORGIO_8BIT to Credits(listOf(
                "Music - Hawkelele",
                "Based on 'Giorgio Cubetti' by Singalek",
                "Cover of 'If I Could, I Would' - MINDME"
            ),
            ytChannel = "IlCoroDiMammonk"
        ),
        BINOBINOOO to Credits(ytChannel = "SingalekSMW"),
        PADRE_MAMMONK to Credits(listOf(
                "Vocals - Emoon LeStrange, HunterProduction, KalarFenrir, Singalek",
                "Chorus composed by Emoon LeStrange",
                "Cover of 'All is Found' - Evan Rachel Wood"
            ),
            ytChannel = "IlCoroDiMammonk"
        ),
        ABBANDONATI to Credits(listOf(
                "Lyrics - Cydonia",
                "Vocals - Ako",
                "Music, Mix & Master - R-E-M"
            ),
            ytChannel = "rem.producer"
        ),
        MISSIVA_NELL_OMBRA to Credits(listOf(
                "Vocals - Singalek",
                "Music - Hawkelele, Monnui, AndreWharn, HunterProduction, 't Hooft",
                "Cover of 'Sleepsong' - Secret Garden"
            ),
            ytChannel = "IlCoroDiMammonk"
        ),
        OURSTEPS to Credits(listOf(
                "Vocals - Singalek, Blessing Of No One",
                "Music - Lena Raine, IridioSound, Avage",
                "Cover of 'Pigstep' - Lena Raine"
            ),
            ytChannel = "IlCoroDiMammonk"
        ),
        ODI_ET_AMO to Credits(listOf(
                "Vocals - Jova117, Emoon LeStrange",
                "Music - Hawkelele, Jova117"
            ),
            ytChannel = "IlCoroDiMammonk"
        ),
        ANCORA_QUI to Credits(listOf(
                "Vocals - Avage, Emoon LeStrange, Singalek",
                "Music - Avage"
            ),
            ytChannel = "IlCoroDiMammonk"
        ),
        BALLATA_DEL_RESPAWN to Credits(listOf(
                "Vocals - Monnui",
                "Music - Monnui, Singalek, IridioSound"
            ),
            ytChannel = "IlCoroDiMammonk"
        ),
        CACO_CACO to Credits(listOf(
                "Vocals - Blessing Of No One, HunterProduction, KalarFenrir, Singalek",
                "Music - HunterProduction"
            ),
            ytChannel = "IlCoroDiMammonk"
        ),
        PESCI_STRANI to Credits(listOf(
                "Vocals - Emoon LeStrange, Singalek",
                "Music - Hawkelele, Emoon LeStrange, Monnui, Singalek"
            ),
            ytChannel = "IlCoroDiMammonk"
        )
    )

    // Create data in datagen
    fun bootstrap(ctx: BootstrapContext<JukeboxSong>) {
        register(ctx, SEGA_DI_NIENTE,       GrowssethSounds.DISC_SEGA_DI_NIENTE, 119)
        register(ctx, GIORGIO_CUBETTI,      GrowssethSounds.DISC_GIORGIO_CUBETTI, 161)
        register(ctx, GIORGIO_LOFI,         GrowssethSounds.DISC_GIORGIO_LOFI, 295)
        register(ctx, GIORGIO_LOFI_INST,    GrowssethSounds.DISC_GIORGIO_LOFI_INST, 295)
        register(ctx, GIORGIO_FINDING_HOME, GrowssethSounds.DISC_GIORGIO_FINDING_HOME, 186)
        register(ctx, GIORGIO_8BIT,         GrowssethSounds.DISC_GIORGIO_8BIT, 152)
        register(ctx, BINOBINOOO,           GrowssethSounds.DISC_BINOBINOOO, 98)
        register(ctx, PADRE_MAMMONK,        GrowssethSounds.DISC_PADRE_MAMMONK, 74)
        register(ctx, ABBANDONATI,          GrowssethSounds.DISC_ABBANDONATI, 145)
        register(ctx, MISSIVA_NELL_OMBRA,   GrowssethSounds.DISC_MISSIVA_NELL_OMBRA, 329)
        register(ctx, OURSTEPS,             GrowssethSounds.DISC_OURSTEPS, 154)
        register(ctx, ODI_ET_AMO,           GrowssethSounds.DISC_ODI_ET_AMO, 191)
        register(ctx, ANCORA_QUI,           GrowssethSounds.DISC_ANCORA_QUI, 145)
        register(ctx, BALLATA_DEL_RESPAWN,  GrowssethSounds.DISC_BALLATA_DEL_RESPAWN, 266)
        register(ctx, CACO_CACO,            GrowssethSounds.DISC_CACO_CACO, 145)
        register(ctx, PESCI_STRANI,         GrowssethSounds.DISC_PESCI_STRANI, 157)
    }

    private fun register(
        context: BootstrapContext<JukeboxSong>,
        key: ResourceKey<JukeboxSong>,
        soundEvent: Holder.Reference<SoundEvent>,
        lengthInSeconds: Int,
        comparatorOutput: Int = 7
    ) {
        context.register(
            key, JukeboxSong(
                soundEvent, Component.translatable(Util.makeDescriptionId("jukebox_song", key.location())),
                lengthInSeconds.toFloat(), comparatorOutput
            )
        )
    }

    data class Credits(val credits: List<String> = listOf(), val ytChannel: String? = null) {
        fun getTooltip(): List<Component>? {
            if (credits.isEmpty() && ytChannel == null) return null
            val out = mutableListOf<Component>()

            if (FxUtils.hasShiftDown()) {
                out.addAll(credits
                    .flatMap { splitStringToMaxLength(it, 30).mapIndexed { index, s -> if (index >= 1) "  $s" else s } }
                    .map { Component.literal(it).withStyle(ChatFormatting.BLUE) }
                )
                if (ytChannel != null) {
                    out.add(Component.literal("YT: @$ytChannel").withStyle(ChatFormatting.RED))
                }
            } else {
                out.add(Component.translatable("item.growsseth.authors.pressShift").withStyle(
                    ChatFormatting.BLUE))
            }
            return out
        }

        private fun splitStringToMaxLength(input: String, maxLength: Int): List<String> {
            val words = input.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""

            for (word in words) {
                if (currentLine.length + word.length > maxLength) {
                    lines.add(currentLine.trim())
                    currentLine = word
                } else {
                    if (currentLine.isNotEmpty()) {
                        currentLine += " "
                    }
                    currentLine += word
                }
            }

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.trim())
            }

            return lines
        }
    }
}