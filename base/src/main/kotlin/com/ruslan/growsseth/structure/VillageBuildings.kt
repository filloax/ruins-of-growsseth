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
import net.minecraft.world.level.levelgen.structure.pools.LegacySinglePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList
import com.mojang.datafixers.util.Pair;

typealias BuildingKey = String

object VillageBuildings {
    val houseEntries = mutableMapOf<BuildingKey, MutableList<VillageEntry>>()

    const val CATEGORY_GOLEM_HOUSE:  BuildingKey = "golem_house" // added to pool and used by advancements

    const val DEFAULT_GOLEM_WEIGHT = 3

    val DESERT_GOLEM    = register("desert_golem_house", CATEGORY_GOLEM_HOUSE, "desert", "houses", DEFAULT_GOLEM_WEIGHT)
    val PLAINS_GOLEM    = register("plains_golem_house", CATEGORY_GOLEM_HOUSE, "plains", "houses", DEFAULT_GOLEM_WEIGHT)
    val TAIGA_GOLEM     = register("taiga_golem_house",  CATEGORY_GOLEM_HOUSE, "taiga", "houses", DEFAULT_GOLEM_WEIGHT)
    val SAVANNA_GOLEM   = register("savanna_golem_house", CATEGORY_GOLEM_HOUSE, "savanna", "houses", DEFAULT_GOLEM_WEIGHT)
    val SNOWY_GOLEM     = register("snowy_golem_house", CATEGORY_GOLEM_HOUSE, "snowy", "houses", DEFAULT_GOLEM_WEIGHT)

    fun onServerStarted(server: MinecraftServer) {
        val shouldAddBuildings = com.ruslan.growsseth.config.StructureConfig.golemHouseEnabled
                && !GrowssethWorldPreset.isGrowssethPreset(server)

        if (!shouldAddBuildings) return

        val templatePools: Registry<StructureTemplatePool> = server.registryAccess().registry(Registries.TEMPLATE_POOL).get()
        val processorLists: Registry<StructureProcessorList> = server.registryAccess().registry(Registries.PROCESSOR_LIST).get()

        houseEntries[CATEGORY_GOLEM_HOUSE]!!.forEach { entry ->
            addBuildingToPool(templatePools, processorLists, entry.parentPool, entry.normalTemplate, entry.weight)
            addBuildingToPool(templatePools, processorLists, entry.parentZombiePool, entry.zombieTemplate, entry.weight)
        }
    }

    fun addBuildingToPool(
        templatePoolRegistry: Registry<StructureTemplatePool>, processorListRegistry: Registry<StructureProcessorList>,
        poolId: ResourceLocation, poolPieceId: ResourceLocation,
        weight: Int,
    ) {
        val pool: StructureTemplatePool = templatePoolRegistry.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, poolId))
        val emptyProcessor = ResourceLocation.fromNamespaceAndPath("minecraft", "empty")     // some houses have mossify 10% percent, but for now we keep it simple
        val processorHolder: Holder<StructureProcessorList> = processorListRegistry.getHolderOrThrow(
            ResourceKey.create(
                Registries.PROCESSOR_LIST, emptyProcessor
            )
        )
        val piece = LegacySinglePoolElement.legacy(poolPieceId.toString()).apply(StructureTemplatePool.Projection.RIGID)
        for (i in 0 until weight) {
            pool.templates.add(piece)
        }
        pool.rawTemplates = ArrayList(pool.rawTemplates).also { it.add(Pair(piece, weight)) }
    }

    private fun register(name: String, category: BuildingKey, kind: String, pool: String, weight: Int): VillageEntry {
        val prefix = "village"
        val templateName = "$prefix/$kind/$name"
        val templateNameZombie = "$prefix/$kind/${name}_zombie"
        val poolName = "$prefix/$name".replace("house", "houses")
        val poolNameZombie = "$prefix/${name}_zombie".replace("house", "houses")
        return VillageEntry(
            kind,
            ResourceLocation.fromNamespaceAndPath("minecraft", "$prefix/$kind/$pool"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "$prefix/$kind/zombie/$pool"),
            resLoc(poolName),
            resLoc(poolNameZombie),
            resLoc(templateName),
            resLoc(templateNameZombie),
            weight,
        ).also {
            houseEntries.getOrPut(category, ::mutableListOf).add(it)
        }
    }

    data class VillageEntry(
        val kind: String,
        val parentPool: ResourceLocation,
        val parentZombiePool: ResourceLocation,
        val normalPool: ResourceLocation,
        val zombiePool: ResourceLocation,
        val normalTemplate: ResourceLocation,
        val zombieTemplate: ResourceLocation,
        val weight: Int,
    )
}