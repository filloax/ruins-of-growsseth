package com.ruslan.growsseth.sound

import com.filloax.fxlib.api.registration.RegistryHolderDelegate
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

class GrowssethSounds {

    companion object {
        val DISC_SEGA_DI_NIENTE by make("disc_sega_di_niente")
        val DISC_GIORGIO_CUBETTI by make("disc_giorgio_cubetti")
        val DISC_GIORGIO_LOFI by make("disc_giorgio_lofi")
        val DISC_GIORGIO_LOFI_INST by make("disc_giorgio_lofi_inst")
        val DISC_GIORGIO_FINDING_HOME by make("disc_giorgio_finding_home")
        val DISC_GIORGIO_8BIT by make("disc_giorgio_8bit")
        val DISC_BINOBINOOO by make("disc_binobinooo")
        val DISC_PADRE_MAMMONK by make("disc_padre_mammonk")
        val DISC_ABBANDONATI by make("disc_abbandonati")
        val DISC_MISSIVA_NELL_OMBRA by make("disc_missiva_nell_ombra")
        val DISC_OURSTEPS by make("disc_oursteps")
        val DISC_ODI_ET_AMO by make("disc_odi_et_amo")
        val DISC_ANCORA_QUI by make("disc_ancora_qui")
        val DISC_BALLATA_DEL_RESPAWN by make("disc_ballata_del_respawn")
        val DISC_CACO_CACO by make("disc_caco_caco")
        val DISC_PESCI_STRANI by make("disc_pesci_strani")

        val DEFLECT_ARROW_SOUND by make("deflect_arrow_sound")
        val RESEARCHER_HORN_SOUND by make("researcher_horn_sound")

        val RESEARCHER_YES by make("researcher.yes")
        val RESEARCHER_NO by make("researcher.no")
        val RESEARCHER_TRADE by make("researcher.trade")
        val RESEARCHER_AMBIENT by make("researcher.ambient")
        val RESEARCHER_HURT by make("researcher.hurt")
        val RESEARCHER_DEATH by make("researcher.death")

        val ZOMBIE_RESEARCHER_AMBIENT by make("zombie_researcher.ambient")
        val ZOMBIE_RESEARCHER_HURT by make("zombie_researcher.hurt")
        val ZOMBIE_RESEARCHER_DEATH by make("zombie_researcher.death")

        private val all = mutableMapOf<Identifier, RegistryHolderDelegate<SoundEvent>>()

        private fun make(name: String) =
            RegistryHolderDelegate(resLoc(name), SoundEvent.createVariableRangeEvent(resLoc(name)))
            .apply {
                if (all.containsKey(id))
                    throw IllegalArgumentException("Sound event $name already registered!")
                all[id] = this
            }

        fun registerSoundEvents(registrator: (Identifier, SoundEvent) -> Holder<SoundEvent>) {
            all.values.forEach{
                it.initHolder(registrator(it.id, it.value))
            }
        }
    }
}