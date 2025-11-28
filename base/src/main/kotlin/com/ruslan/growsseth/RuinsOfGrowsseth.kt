package com.ruslan.growsseth

import com.filloax.fxlib.api.FxLibServices
import com.filloax.fxlib.api.platform.ServiceUtil
import com.ruslan.growsseth.compat.ModCompatChecker
import com.ruslan.growsseth.config.GrowssethConfigHandler
import com.ruslan.growsseth.dialogues.ResearcherDialogueApiListener
import com.ruslan.growsseth.entity.researcher.CustomRemoteDiaries
import com.ruslan.growsseth.entity.researcher.ResearcherDiaryComponent
import com.ruslan.growsseth.entity.researcher.trades.GameMasterResearcherTradesProvider
import com.ruslan.growsseth.http.GrowssethApi
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.network.GrowssethPackets
import com.ruslan.growsseth.resource.MusicCommon
import com.ruslan.growsseth.structure.*
import com.ruslan.growsseth.utils.GrowssethLogger
import com.ruslan.growsseth.utils.loadPropertiesFile
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager


abstract class RuinsOfGrowsseth {
    companion object {
        const val MOD_ID = "growsseth"
        const val MOD_NAME = "Ruins of Growsseth"

        @JvmField
        val LOGGER = GrowssethLogger(LogManager.getLogger(MOD_NAME))

        fun logDev(level: Level, message: String) {
            if (FxLibServices.platform.isDevEnvironment()) {
                LOGGER.log(level, message)
            }
        }

        val cydoniaProperties = loadPropertiesFile("cydonia.properties")
        val cydoniaMode: Boolean = cydoniaProperties["cydoniaMode"]!!.toBoolean()

        val modCompat = ServiceUtil.findService(ModCompatChecker::class.java)
    }

    final fun initialize() {
        LOGGER.info("Initializing")

        ModEvents.get().initCallbacks()

        GrowssethApi.current.init()
        initRegistries()
        RemoteStructures.init()
        CustomRemoteDiaries.init()
        RemoteStructureBooks.init()
        GameMasterResearcherTradesProvider.subscribe()
        GrowssethExtraEvents.init()
        ResearcherDialogueApiListener.init()
        ResearcherDiaryComponent.init()
        GrowssethPackets.registerPacketsC2S()
        GrowssethPackets.registerPacketsS2C()

        initItemGroups()
        registerResourceListeners()

        GrowssethConfigHandler.initConfig()

        MusicCommon.initCheck()

        if (cydoniaMode)
            LOGGER.info("Cydonia mode enabled, structures won't spawn and API v1 will be used")

        // Mod compat
        if (modCompat.isCobblemonLoaded) {
            if (!modCompat.isRCTTrainerApiLoaded) {
                LOGGER.error("Cobblemon loaded but Cobblemon RCT trainer API not loaded, researcher as trainer won't work")
            }
        }

        LOGGER.info("Initialized! :saidogPipo: :saidogRitto: :saidogMax:")
    }

    abstract fun initItemGroups()
    abstract fun registerResourceListeners()
    abstract fun initRegistries()
}