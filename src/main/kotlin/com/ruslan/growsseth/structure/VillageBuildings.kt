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


object VillageBuildings {
    val houseEntries = mutableMapOf<String, MutableList<VillageEntry>>()

    const val CATEGORY_GOLEM_STREET = "golem_street" // added to pool
    const val CATEGORY_GOLEM_HOUSE = "golem_house" // already in pool referenced by street, used by advancements

    const val GOLEM_WEIGHT = 1      // golem street pools only have one house each

    // The weights are balanced for each biome (values adjusted by trial and error, sadly there's not much choice with the vanilla weights)
    val DESERT_GOLEM_STREET    = register("desert_golem_street", CATEGORY_GOLEM_STREET, "desert", "streets", 1)
    val PLAINS_GOLEM_STREET    = register("plains_golem_street", CATEGORY_GOLEM_STREET, "plains", "streets", 2)
    val TAIGA_GOLEM_STREET     = register("taiga_golem_street",  CATEGORY_GOLEM_STREET, "taiga", "streets", 3)
    val SAVANNA_GOLEM_STREET   = register("savanna_golem_street", CATEGORY_GOLEM_STREET, "savanna", "streets", 3)
    val SNOWY_GOLEM_STREET     = register("snowy_golem_street", CATEGORY_GOLEM_STREET, "snowy", "streets", 1)

    val DESERT_GOLEM    = register("desert_golem_house", CATEGORY_GOLEM_HOUSE, "desert", "house", GOLEM_WEIGHT)
    val PLAINS_GOLEM    = register("plains_golem_house", CATEGORY_GOLEM_HOUSE, "plains", "house", GOLEM_WEIGHT)
    val TAIGA_GOLEM     = register("taiga_golem_house",  CATEGORY_GOLEM_HOUSE, "taiga", "house", GOLEM_WEIGHT)
    val SAVANNA_GOLEM   = register("savanna_golem_house", CATEGORY_GOLEM_HOUSE, "savanna", "house", GOLEM_WEIGHT)
    val SNOWY_GOLEM     = register("snowy_golem_house", CATEGORY_GOLEM_HOUSE, "snowy", "house", GOLEM_WEIGHT)

    fun onServerStarted(server: MinecraftServer) {
        val shouldAddBuildings = StructureConfig.golemHouseEnabled
                && !GrowssethWorldPreset.isGrowssethPreset(server)

        if (!shouldAddBuildings) return

        val templatePools: Registry<StructureTemplatePool> = server.registryAccess().registry(Registries.TEMPLATE_POOL).get()
        val processorLists: Registry<StructureProcessorList> = server.registryAccess().registry(Registries.PROCESSOR_LIST).get()

        houseEntries[CATEGORY_GOLEM_STREET]!!.forEach { entry ->
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
        val emptyProcessor = ResourceLocation("minecraft", "empty")
        val processorHolder: Holder<StructureProcessorList> = processorListRegistry.getHolderOrThrow(
            ResourceKey.create(
                Registries.PROCESSOR_LIST, emptyProcessor
            )
        )
        val piece = LegacySinglePoolElement.legacy(poolPieceId.toString()).apply(StructureTemplatePool.Projection.TERRAIN_MATCHING)
        for (i in 0 until weight) {
            pool.templates.add(piece)
        }
        pool.rawTemplates = ArrayList(pool.rawTemplates).also { it.add(Pair(piece, weight)) }
    }

    private fun register(name: String, category: String, kind: String, pool: String, weight: Int): VillageEntry {
        val prefix = "village"
        val templateName = "$prefix/$kind/$name"
        val templateNameZombie = "$prefix/$kind/${name}_zombie"
        val poolName = "$prefix/$name".replace("house", "houses")
        val poolNameZombie = "$prefix/${name}_zombie".replace("house", "houses")
        return VillageEntry(
            kind,
            ResourceLocation("minecraft", "$prefix/$kind/$pool"),
            ResourceLocation("minecraft", "$prefix/$kind/zombie/$pool"),
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