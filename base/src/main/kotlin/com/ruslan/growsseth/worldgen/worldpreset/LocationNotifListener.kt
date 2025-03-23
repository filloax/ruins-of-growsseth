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

        // Filled after loading, at runtime, as otherwise FxLib could have not finished loading yet
        val LOCALISED_PLACES_DATA : List<LocationData> get() = getLocalised()

        private val PLACES_DATA : MutableList<LocationData> = mutableListOf()
        private val _LOCALISED_PLACES_DATA : MutableList<LocationData> = mutableListOf()

        private val nextReloadConsumers = mutableListOf<(List<LocationData>) -> Unit>()
        var loaded = false
            private set

        fun onNextReload(consumer: (List<LocationData>) -> Unit) {
            nextReloadConsumers.add(consumer)
        }

        private fun getLocalised(): List<LocationData> {
            if (_LOCALISED_PLACES_DATA.isEmpty()) {
                _LOCALISED_PLACES_DATA.addAll(PLACES_DATA.map { it.processLocationName() })
            }
            return _LOCALISED_PLACES_DATA
        }
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        PLACES_DATA.clear()
        _LOCALISED_PLACES_DATA.clear()

        loader.forEach { (fileIdentifier, jsonElement) ->
            var last: JsonElement? = null
            try {
                jsonElement.jsonArray.forEach {
                    last = it
                    val entry = JSON.decodeFromJsonElement(LocationData.serializer(), it)

                    PLACES_DATA.add(entry)
                }
            } catch (e: Exception) {
                RuinsOfGrowsseth.LOGGER.error("Couldn't parse places file {} at {}", fileIdentifier, last, e)
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