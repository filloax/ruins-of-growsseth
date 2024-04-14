package com.ruslan.growsseth.structure

import com.filloax.fxlib.EventUtil
import com.filloax.fxlib.FxLibServices
import com.filloax.fxlib.nbt.*
import com.filloax.fxlib.structure.FixedStructureGeneration
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.http.GrowssethApi
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Rotation

object RemoteStructures {
    // Note: for now assumes it's in the overworld

    val STRUCTS_TO_SPAWN_BY_ID: Map<String, StructureSpawnData>
        get() = structsToSpawnById


    private val structsToSpawnById = mutableMapOf<String, StructureSpawnData>()
    private val fixedStructureGeneration: FixedStructureGeneration = FxLibServices.fixedStructureGeneration

    fun init() {
        GrowssethApi.current.subscribe { api, server ->
            EventUtil.runWhenServerStarted(server) { _ ->
                val newSpawns = api.structureSpawns

                structsToSpawnById.clear()

                newSpawns.forEach {
                    val id = ResourceLocation(it.structureId)
                    if (it.active && id.namespace != Constants.EVENT_NAMESPACE) {
                        val structureRef = server.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id)
                        if (structureRef == null) {
                            RuinsOfGrowsseth.LOGGER.error("Cannot queue non-existent structure $id")
                        } else {
                            fixedStructureGeneration.register(server.overworld(), it.name, it.startPos, id, it.rotation ?: Rotation.NONE)
                            structsToSpawnById[it.name] = StructureSpawnData(it.startPos, id, it.name, it.rotation)
                        }
                    }
                }
            }
        }
    }

    data class StructureSpawnData(
        val pos: BlockPos,
        val structure: ResourceLocation,
        val spawnId: String,
        val rotation: Rotation? = null
    )
}