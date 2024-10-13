package com.ruslan.growsseth.client.worldpreset

import com.filloax.fxlib.api.secondsToTicks
import com.filloax.fxlib.api.vec3
import com.ruslan.growsseth.client.gui.locationtitles.LocationTitlesController
import com.ruslan.growsseth.utils.notNull
import com.ruslan.growsseth.worldgen.worldpreset.LocationData
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos

object GrowssethWorldPresetClient {
    val LOCATION_DATA: List<LocationData>
        get() = locationData

    private val MIN_DELAY_TICKS = 1f.secondsToTicks()
    private const val MIN_DISTANCE_UPDATE = 6

    private val locationData = mutableListOf<LocationData>()
    private var lastShowTime: Int = -1
    private var lastPos: BlockPos = BlockPos(0,0,0)

    fun initLocationData(locationData: List<LocationData>) {
        this.locationData.clear()
        this.locationData.addAll(locationData.filter { !it.hidden && notNull(it.boundingBox) })
    }

    private fun locationTitlesController() = LocationTitlesController.get()

    object Callbacks {
        fun onClientTick(client: Minecraft) {
            if (!com.ruslan.growsseth.config.ClientConfig.enableLocationTitles) return
            val player = client.player ?: return

            if (lastShowTime > player.tickCount) lastShowTime = 0
            if (player.tickCount - lastShowTime < MIN_DELAY_TICKS) return

            val pos = player.blockPosition()

            if (pos.distManhattan(lastPos) > MIN_DISTANCE_UPDATE) {
                val fPos = player.position();
                val closestInside = LOCATION_DATA
                    .filter { it.boundingBox?.let { vol -> vol.contains(fPos) && !vol.contains(lastPos.vec3()) } == true }
                    .minByOrNull { it.centerPos.distanceToSqr(fPos) }
                if (closestInside != null) {
                    locationTitlesController().showLocationTitle(closestInside.name)
                    lastShowTime = player.tickCount
                }
                lastPos = pos
            }
        }
    }
}