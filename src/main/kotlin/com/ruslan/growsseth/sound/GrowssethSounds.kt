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
        val DISC_GIORGIO_8BIT = registerSoundEvent("disc_giorgio_8bit")
        val DISC_BINOBINOOO = registerSoundEvent("disc_binobinooo")
        val DISC_PADRE_MAMMONK = registerSoundEvent("disc_padre_mammonk")
        val DISC_ABBANDONATI = registerSoundEvent("disc_abbandonati")
        val DISC_MISSIVA_NELL_OMBRA = registerSoundEvent("disc_missiva_nell_ombra")

        val DISC_OURSTEPS = registerSoundEvent("disc_oursteps")
        val DISC_ODI_ET_AMO = registerSoundEvent("disc_odi_et_amo")
        val DISC_ANCORA_QUI = registerSoundEvent("disc_ancora_qui")
        val DISC_BALLATA_DEL_RESPAWN = registerSoundEvent("disc_ballata_del_respawn")
        val DISC_CACO_CACO = registerSoundEvent("disc_caco_caco")
        val DISC_PESCI_STRANI = registerSoundEvent("disc_pesci_strani")

        val DEFLECT_ARROW_SOUND = registerSoundEvent("deflect_arrow_sound")
        val RESEARCHER_HORN_SOUND = registerSoundEventHolder("researcher_horn_sound")

        val RESEARCHER_YES = registerSoundEvent("researcher.yes")
        val RESEARCHER_NO = registerSoundEvent("researcher.no")
        val RESEARCHER_TRADE = registerSoundEvent("researcher.trade")
        val RESEARCHER_AMBIENT = registerSoundEvent("researcher.ambient")
        val RESEARCHER_HURT = registerSoundEvent("researcher.hurt")
        val RESEARCHER_DEATH = registerSoundEvent("researcher.death")

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