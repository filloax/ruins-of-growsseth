package com.ruslan.growsseth.item

import com.ruslan.growsseth.sound.GrowssethSounds
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.Util
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.JukeboxSong

object GrowssethJukeboxSongs {
    val SEGA_DI_NIENTE 		= create("sega_di_niente")
    val GIORGIO_CUBETTI 		= create("giorgio_cubetti")
    val GIORGIO_LOFI 			= create("giorgio_lofi")
    val GIORGIO_LOFI_INST 		= create("giorgio_lofi_inst")
    val GIORGIO_FINDING_HOME 	= create("giorgio_finding_home")
    val GIORGIO_8BIT		 	= create("giorgio_8bit")
    val BINOBINOOO 			= create("binobinooo")
    val PADRE_MAMMONK 			= create("padre_mammonk")
    val ABBANDONATI 			= create("abbandonati")
    val MISSIVA_NELL_OMBRA 	= create("missiva_nell_ombra")
    val OURSTEPS 				= create("oursteps")
    val ODI_ET_AMO 			= create("odi_et_amo")
    val ANCORA_QUI 			= create("ancora_qui")
    val BALLATA_DEL_RESPAWN 	= create("ballata_del_respawn")
    val CACO_CACO 				= create("caco_caco")
    val PESCI_STRANI 			= create("pesci_strani")

    private fun create(id: String) = ResourceKey.create(Registries.JUKEBOX_SONG, resLoc(id))

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
}