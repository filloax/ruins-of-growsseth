package com.ruslan.growsseth.worldgen.worldpreset

import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

class LocationNotifListener : KotlinJsonResourceReloadListener(JSON, Constants.PRESET_PLACES_FOLDER) {
    companion object {
        private val JSON = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }

        val PLACES_DATA : MutableList<LocationData> = mutableListOf()

        private val nextReloadConsumers = mutableListOf<(List<LocationData>) -> Unit>()
        var loaded = false
            private set

        fun onNextReload(consumer: (List<LocationData>) -> Unit) {
            nextReloadConsumers.add(consumer)
        }
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        PLACES_DATA.clear()

        loader.forEach { (fileIdentifier, jsonElement) ->
            var last: JsonElement? = null
            try {
                jsonElement.jsonArray.forEach {
                    last = it
                    val entry = JSON.decodeFromJsonElement(LocationData.serializer(), it)

                    PLACES_DATA.add(entry)
                }
            } catch (e: Exception) {
                RuinsOfGrowsseth.LOGGER.error( "Growsseth: Couldn't parse places file {} at {}", fileIdentifier, last, e)
            }
        }

        nextReloadConsumers.forEach { it(PLACES_DATA) }
        nextReloadConsumers.clear()
        loaded = true
    }

    object Callbacks {
        fun onServerStopped(server: MinecraftServer) {
            loaded = false
        }
    }
}