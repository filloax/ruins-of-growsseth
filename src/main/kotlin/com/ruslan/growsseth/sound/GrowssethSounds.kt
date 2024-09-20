package com.ruslan.growsseth.sound

import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

class GrowssethSounds {

    companion object {
        val DISC_SEGA_DI_NIENTE = registerSoundEventHolder("disc_sega_di_niente")
        val DISC_GIORGIO_CUBETTI = registerSoundEventHolder("disc_giorgio_cubetti")
        val DISC_GIORGIO_LOFI = registerSoundEventHolder("disc_giorgio_lofi")
        val DISC_GIORGIO_LOFI_INST = registerSoundEventHolder("disc_giorgio_lofi_inst")
        val DISC_GIORGIO_FINDING_HOME = registerSoundEventHolder("disc_giorgio_finding_home")
        val DISC_GIORGIO_8BIT = registerSoundEventHolder("disc_giorgio_8bit")
        val DISC_BINOBINOOO = registerSoundEventHolder("disc_binobinooo")
        val DISC_PADRE_MAMMONK = registerSoundEventHolder("disc_padre_mammonk")
        val DISC_ABBANDONATI = registerSoundEventHolder("disc_abbandonati")
        val DISC_MISSIVA_NELL_OMBRA = registerSoundEventHolder("disc_missiva_nell_ombra")

        val DISC_OURSTEPS = registerSoundEventHolder("disc_oursteps")
        val DISC_ODI_ET_AMO = registerSoundEventHolder("disc_odi_et_amo")
        val DISC_ANCORA_QUI = registerSoundEventHolder("disc_ancora_qui")
        val DISC_BALLATA_DEL_RESPAWN = registerSoundEventHolder("disc_ballata_del_respawn")
        val DISC_CACO_CACO = registerSoundEventHolder("disc_caco_caco")
        val DISC_PESCI_STRANI = registerSoundEventHolder("disc_pesci_strani")

        val DEFLECT_ARROW_SOUND = registerSoundEventHolder("deflect_arrow_sound")
        val RESEARCHER_HORN_SOUND = registerSoundEventHolder("researcher_horn_sound")

        val RESEARCHER_YES = registerSoundEvent("researcher.yes")
        val RESEARCHER_NO = registerSoundEvent("researcher.no")
        val RESEARCHER_TRADE = registerSoundEvent("researcher.trade")
        val RESEARCHER_AMBIENT = registerSoundEvent("researcher.ambient")
        val RESEARCHER_HURT = registerSoundEvent("researcher.hurt")
        val RESEARCHER_DEATH = registerSoundEvent("researcher.death")

        val ZOMBIE_RESEARCHER_AMBIENT = registerSoundEvent("zombie_researcher.ambient")
        val ZOMBIE_RESEARCHER_HURT = registerSoundEvent("zombie_researcher.hurt")
        val ZOMBIE_RESEARCHER_DEATH = registerSoundEvent("zombie_researcher.death")

        private fun registerSoundEvent(name: String): SoundEvent {
            val id = ResourceLocation.fromNamespaceAndPath(RuinsOfGrowsseth.MOD_ID, name)
            return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))
        }

        private fun registerSoundEventHolder(name: String): Holder.Reference<SoundEvent> {
            val id = ResourceLocation.fromNamespaceAndPath(RuinsOfGrowsseth.MOD_ID, name)
            return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))
        }
    }
}