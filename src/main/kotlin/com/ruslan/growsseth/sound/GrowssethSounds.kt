package com.ruslan.growsseth.sound

import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

class GrowssethSounds {

    companion object {
        val DISC_SEGA_DI_NIENTE = registerSoundEvent("disc_sega_di_niente")
        val DISC_GIORGIO_CUBETTI = registerSoundEvent("disc_giorgio_cubetti")
        val DISC_GIORGIO_LOFI = registerSoundEvent("disc_giorgio_lofi")
        val DISC_GIORGIO_LOFI_INST = registerSoundEvent("disc_giorgio_lofi_inst")
        val DISC_GIORGIO_FINDING_HOME = registerSoundEvent("disc_giorgio_finding_home")
        val DISC_BINOBINOOO = registerSoundEvent("disc_binobinooo")
        val DISC_BINOBINOOO_INST = registerSoundEvent("disc_binobinooo_inst")
        val DISC_PADRE_MAMMONK = registerSoundEvent("disc_padre_mammonk")
        val DISC_ABBANDONATI = registerSoundEvent("disc_abbandonati")
        val DISC_MISSIVA_NELL_OMBRA = registerSoundEvent("disc_missiva_nell_ombra")

        // vanilla ost, but not available as standalone sound event
        val MUSIC_MICE_ON_VENUS = registerSoundEvent("disc_mice_on_venus")
        val MUSIC_INFINITE_AMETHYST = registerSoundEvent("disc_infinite_amethyst")
        val MUSIC_LABYRINTHINE = registerSoundEvent("disc_labyrinthine")

        val DEFLECT_ARROW_SOUND = registerSoundEvent("deflect_arrow_sound")
        val RESEARCHER_HORN_SOUND = registerSoundEventHolder("researcher_horn_sound")

        private fun registerSoundEvent(name: String): SoundEvent {
            val id = ResourceLocation(RuinsOfGrowsseth.MOD_ID, name)
            return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))
        }

        private fun registerSoundEventHolder(name: String): Holder<SoundEvent> {
            val id = ResourceLocation(RuinsOfGrowsseth.MOD_ID, name)
            return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))
        }
    }
}