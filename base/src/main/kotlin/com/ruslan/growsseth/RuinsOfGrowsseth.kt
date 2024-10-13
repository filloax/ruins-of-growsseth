package com.ruslan.growsseth

import com.filloax.fxlib.api.FxLibServices
import com.ruslan.growsseth.advancements.GrowssethCriterions
import com.ruslan.growsseth.config.GrowssethConfigHandler
import com.ruslan.growsseth.dialogues.ResearcherDialogueApiListener
import com.ruslan.growsseth.dialogues.ResearcherDialogueListener
import com.ruslan.growsseth.effect.GrowssethEffects
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.CustomRemoteDiaries
import com.ruslan.growsseth.entity.researcher.ResearcherDiaryComponent
import com.ruslan.growsseth.entity.researcher.trades.GameMasterResearcherTradesProvider
import com.ruslan.growsseth.entity.researcher.trades.TradesListener
import com.ruslan.growsseth.http.GrowssethApi
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.item.GrowssethCreativeModeTabs
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.maps.GrowssethMapDecorations
import com.ruslan.growsseth.networking.GrowssethPackets
import com.ruslan.growsseth.platform.PlatformAbstractions
import com.ruslan.growsseth.resource.MusicCommon
import com.ruslan.growsseth.structure.*
import com.ruslan.growsseth.templates.TemplateListener
import com.ruslan.growsseth.utils.loadPropertiesFile
import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.worldpreset.LocationNotifListener
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Items
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*


abstract class RuinsOfGrowsseth {
    companion object {
        @JvmStatic
        val LOGGER: Logger = LogManager.getLogger()
        const val MOD_ID = "growsseth"
        const val MOD_NAME = "Ruins of Growsseth"

        fun log(level: Level, message: String) {
            LOGGER.log(level, "[$MOD_NAME] $message")
        }

        fun logDev(level: Level, message: String) {
            if (FxLibServices.platform.isDevEnvironment()) {
                LOGGER.log(level, "[$MOD_NAME] $message")
            }
        }

        val cydoniaProperties = loadPropertiesFile("cydonia.properties")
        val cydoniaMode: Boolean = cydoniaProperties["cydoniaMode"]!!.toBoolean()
    }

    final fun initialize() {
        log(Level.INFO, "Initializing")

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

        initItemGroups()
        registerResourceListeners()

        GrowssethConfigHandler.initConfig()

        MusicCommon.initCheck()

        if (cydoniaMode)
            log(Level.INFO, "Cydonia mode enabled, structures won't spawn and API v1 will be used")

        log(Level.INFO, "Initialized! :saidogPipo: :saidogRitto: :saidogMax:")
    }

    abstract fun initItemGroups()
    abstract fun registerResourceListeners()
    abstract fun initRegistries()
}