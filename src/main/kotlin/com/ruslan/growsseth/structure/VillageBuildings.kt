package com.ruslan.growsseth.structure

import com.ruslan.growsseth.config.StructureConfig
import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList
import com.mojang.datafixers.util.Pair;


object VillageBuildings {
    private val houseEntries = mutableMapOf<String, MutableList<VillageEntry>>()

    const val CATEGORY_GOLEM = "golem"

    const val GOLEM_WEIGHT = 1

    val DESERT_GOLEM    = register("desert_golem_street", CATEGORY_GOLEM, "desert", "streets", GOLEM_WEIGHT)
    val PLAINS_GOLEM    = register("plains_golem_street", CATEGORY_GOLEM, "plains", "streets", GOLEM_WEIGHT)
    val TAIGA_GOLEM     = register("taiga_golem_street",  CATEGORY_GOLEM, "taiga", "streets", GOLEM_WEIGHT)
    val SAVANNA_GOLEM   = register("savanna_golem_street", CATEGORY_GOLEM, "savanna", "streets", GOLEM_WEIGHT)
    val SNOWY_GOLEM     = register("snowy_golem_street", CATEGORY_GOLEM, "snowy", "streets", GOLEM_WEIGHT)

    fun onServerStarted(server: MinecraftServer) {
        val shouldAddBuildings = StructureConfig.golemHouseEnabled
                && !GrowssethWorldPreset.isGrowssethPreset(server)

        if (!shouldAddBuildings) return

        val templatePools: Registry<StructureTemplatePool> = server.registryAccess().registry(Registries.TEMPLATE_POOL) .get()
        val processorLists: Registry<StructureProcessorList> = server.registryAccess().registry(Registries.PROCESSOR_LIST).get()

        houseEntries[CATEGORY_GOLEM]!!.forEach { entry ->
            addBuildingToPool(templatePools, processorLists, entry.pool, entry.normal, entry.weight)
            addBuildingToPool(templatePools, processorLists, entry.zombiePool, entry.zombie, entry.weight)
        }
    }

    fun addBuildingToPool(
        templatePoolRegistry: Registry<StructureTemplatePool>, processorListRegistry: Registry<StructureProcessorList>,
        poolId: ResourceLocation, poolPieceId: ResourceLocation,
        weight: Int,
    ) {
        val pool: StructureTemplatePool = templatePoolRegistry.get(poolId) ?: return
        val emptyProcessor = ResourceLocation("minecraft", "empty")
        val processorHolder: Holder<StructureProcessorList> = processorListRegistry.getHolderOrThrow(
            ResourceKey.create(
                Registries.PROCESSOR_LIST, emptyProcessor
            )
        )
        val piece = SinglePoolElement.single(poolPieceId.toString(), processorHolder).apply(StructureTemplatePool.Projection.RIGID)
        for (i in 0 until weight) {
            pool.templates.add(piece)
        }
        pool.rawTemplates = ArrayList(pool.rawTemplates).also { it.add(Pair(piece, weight)) }
    }

    private fun register(name: String, category: String, kind: String, pool: String, weight: Int): VillageEntry {
        val prefix = "village/$kind"
        return VillageEntry(
            kind,
            ResourceLocation("minecraft", "$prefix/$pool"),
            ResourceLocation("minecraft", "$prefix/zombie/$pool"),
            resLoc("$prefix/$name"),
            resLoc("$prefix/${name}_zombie"),
            weight,
        ).also {
            houseEntries.getOrPut(category, ::mutableListOf).add(it)
        }
    }

    data class VillageEntry(
        val kind: String,
        val pool: ResourceLocation,
        val zombiePool: ResourceLocation,
        val normal: ResourceLocation,
        val zombie: ResourceLocation,
        val weight: Int,
    )
}