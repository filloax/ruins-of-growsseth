package com.ruslan.growsseth.compat.cobblemon

import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import com.filloax.fxlib.api.json.toGson
import com.gitlab.srcmc.rctapi.api.models.TrainerModel
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

private val JSON = Json

private val GSON = CobblemonRCTCompat.RCT.gsonBuilder()
    .disableHtmlEscaping()
    .setPrettyPrinting()
    .create()

object CobblemonRCTListener : KotlinJsonResourceReloadListener(JSON, Constants.COMPAT_COBBLEMON_FOLDER) {
    var TRAINER_DATA: Map<TrainerTeam, TrainerModel> = mutableMapOf()
        private set

    private val TRAINER_FILENAME = "trainer_data"

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        if (RuinsOfGrowsseth.modCompat.isRCTTrainerApiLoaded) {
            RuinsOfGrowsseth.LOGGER.info("Loading Cobblemon RCT trainer data...")
        } else {
            return
        }

        val trainerFile = loader.filter { it.key.path.startsWith(TRAINER_FILENAME) }.values.firstOrNull()
        if (trainerFile == null) {
            RuinsOfGrowsseth.LOGGER.error("No trainer data found!")
            return
        }

        TRAINER_DATA = trainerFile.jsonObject.map { (key, value) ->
            // decode JSON key into enum using @SerialName
            val enumKey = Json.decodeFromString(TrainerTeam.serializer(), "\"$key\"")

            // RCT wants us to use its GSON instance
            val model = GSON.fromJson(value.toGson(), TrainerModel::class.java)

            enumKey to model
        }.toMap()

        if (CobblemonRCTCompat.isServerActive) {
            RuinsOfGrowsseth.LOGGER.info("Reloading RCT trainer registry NPCs...")
            CobblemonRCTCompat.RCT.trainerRegistry.clearNPCs()
            TRAINER_DATA.forEach { (team, model) ->
                CobblemonRCTCompat.RCT.trainerRegistry.registerNPC(team.toString(), model)
            }
        }
        RuinsOfGrowsseth.LOGGER.info("Trainer data load done!")
    }
}